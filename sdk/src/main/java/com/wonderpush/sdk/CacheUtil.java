package com.wonderpush.sdk;

import android.net.TrafficStats;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

class CacheUtil {

    protected static final int MAX_SOUND_FILE_SIZE = 1 * 1024 * 1024; // 1 MB
    protected static final int MAX_LARGEICON_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    protected static final int MAX_BIGPICTURE_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    protected static final int MAX_SOUND_CACHE_SIZE = 10 * MAX_SOUND_FILE_SIZE;
    protected static final int MAX_LARGEICON_CACHE_SIZE = 5 * MAX_LARGEICON_FILE_SIZE;
    protected static final int MAX_BIGPICTURE_CACHE_SIZE = 3 * MAX_BIGPICTURE_FILE_SIZE;

    static class FetchWork {
        final Uri uri;
        final int maxFileSize;
        final String cacheSubfolder;
        final int maxCacheSize;
        final String logPrefix;

        public FetchWork(Uri uri, int maxFileSize, String cacheSubfolder, int maxCacheSize, String logPrefix) {
            this.uri = uri;
            this.maxFileSize = maxFileSize;
            this.cacheSubfolder = cacheSubfolder;
            this.maxCacheSize = maxCacheSize;
            this.logPrefix = logPrefix;
        }

        public File execute() {
            return doFetch(this);
        }
    }

    static class FetchResult {
        private final File result;
        private final FetchWork work;

        private FetchResult(File result, FetchWork work) {
            this.result = result;
            this.work = work;
        }

        public static FetchResult immediate(@Nullable File result) {
            return new FetchResult(result, null);
        }

        public static FetchResult workTask(@NonNull FetchWork work) {
            return new FetchResult(null, work);
        }

        public boolean needsWork() {
            return work != null;
        }

        public File getResult() {
            return result;
        }

        public FetchWork getWork() {
            return work;
        }
    }

    protected static FetchResult fetchSound(Uri uri, String logPrefix) {
        return fetch(new FetchWork(uri, MAX_SOUND_FILE_SIZE, "sounds", MAX_SOUND_CACHE_SIZE, logPrefix));
    }

    protected static FetchResult fetchLargeIcon(Uri uri, String logPrefix) {
        return fetch(new FetchWork(uri, MAX_LARGEICON_FILE_SIZE, "largeIcons", MAX_LARGEICON_CACHE_SIZE, logPrefix));
    }

    protected static FetchResult fetchBigPicture(Uri uri, String logPrefix) {
        return fetch(new FetchWork(uri, MAX_BIGPICTURE_FILE_SIZE, "bigPictures", MAX_BIGPICTURE_CACHE_SIZE, logPrefix));
    }

    private static FetchResult fetch(FetchWork work) {
        if (Looper.getMainLooper() != null && Looper.getMainLooper().getThread() == Thread.currentThread()) {
            return FetchResult.workTask(work);
        }
        File cached = getCachedFile(work);
        if (cached == null || isUsable(work, cached)) {
            return FetchResult.immediate(cached);
        } else {
            return FetchResult.workTask(work);
        }
    }

    private static boolean isUsable(FetchWork work, @NonNull File cached) {
        // TODO handle caching
        return cached.exists();
    }

    private static File getCachedFile(FetchWork work) {
        String scheme = work.uri.getScheme() == null ? null : work.uri.getScheme().toLowerCase(Locale.ROOT);
        if ("http".equals(scheme) || "https".equals(scheme)) {
            try {
                String filename = Integer.toHexString(work.uri.toString().hashCode());
                File cacheLargeIconsDir = new File(WonderPush.getApplicationContext().getCacheDir(), work.cacheSubfolder);
                cacheLargeIconsDir.mkdirs();
                final File cached = new File(cacheLargeIconsDir, filename);
                return cached;
            } catch (Exception ex) {
                Log.e(WonderPush.TAG, work.logPrefix + ": Failed to fetch from URI " + work.uri, ex);
            }
        }
        return null;
    }

    private static File doFetch(FetchWork work) {
        try {
            File cached = getCachedFile(work); // returns null on invalid work request
            if (cached != null && !isUsable(work, cached)) {
                boolean success = false;
                try {
                    WonderPush.logDebug(work.logPrefix + ": Will open URL: " + work.uri);
                    TrafficStats.setThreadStatsTag(Process.myTid());
                    URLConnection conn = new URL(work.uri.toString()).openConnection();
                    InputStream is = (InputStream) conn.getContent();
                    WonderPush.logDebug(work.logPrefix + ": Content-Type: " + conn.getContentType());
                    WonderPush.logDebug(work.logPrefix + ": Content-Length: " + conn.getContentLength() + " bytes");
                    if (conn.getContentLength() > work.maxFileSize) {
                        throw new RuntimeException(work.logPrefix + " file too large (" + conn.getContentLength() + " is over " + work.maxFileSize + " bytes)");
                    }

                    FileOutputStream outputStream = new FileOutputStream(cached);
                    int read, ttl = 0;
                    byte[] buffer = new byte[2048];
                    while ((read = is.read(buffer)) != -1) {
                        ttl += read;
                        if (ttl > work.maxFileSize) {
                            throw new RuntimeException(work.logPrefix + " file too large (max " + work.maxFileSize + " bytes allowed)");
                        }
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.close();
                    is.close();
                    success = true;
                    WonderPush.logDebug(work.logPrefix + ": Finished reading " + ttl + " bytes");
                } catch (IOException ex) {
                    Log.e(WonderPush.TAG, "Error while fetching resource " + work.uri, ex);
                }

                if (!success) {
                    cached.delete();
                    cached = null;
                }
                expireCache(work.cacheSubfolder, work.maxCacheSize);
            }
            return cached;
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, work.logPrefix + ": Failed to fetch from URI " + work.uri, ex);
            return null;
        }
    }

    private static void expireCache(String cacheSubfolder, int maxCacheSize) {
        File dir = new File(WonderPush.getApplicationContext().getCacheDir(), cacheSubfolder);
        if (!dir.isDirectory()) return;

        // Sort from most recently modified to least recently modified
        File[] files = dir.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                // TODO Once minSdkVersion is 19, we can return Long.compare(rhs.lastModified(), lhs.lastModified())
                long lvalue = lhs.lastModified();
                long rvalue = rhs.lastModified();
                if (rvalue == lvalue) return 0;
                if (rvalue < lvalue) return -1;
                return 1;
            }
        });

        int totalSize = 0;
        for (File file : files) {
            totalSize += file.length();
            // Delete any older files above the desired limit
            if (totalSize > maxCacheSize) {
                file.delete();
            }
        }
    }

}

package com.wonderpush.sdk;

import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
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

    protected static File fetchSound(Uri uri, String logPrefix) {
        return fetch(uri, MAX_SOUND_FILE_SIZE, "sounds", MAX_SOUND_CACHE_SIZE, logPrefix);
    }

    protected static File fetchLargeIcon(Uri uri, String logPrefix) {
        return fetch(uri, MAX_LARGEICON_FILE_SIZE, "largeIcons", MAX_LARGEICON_CACHE_SIZE, logPrefix);
    }

    protected static File fetchBigPicture(Uri uri, String logPrefix) {
        return fetch(uri, MAX_BIGPICTURE_FILE_SIZE, "bigPictures", MAX_BIGPICTURE_CACHE_SIZE, logPrefix);
    }

    private static File fetch(Uri uri, int maxFileSize, String cacheSubfolder, int maxCacheSize, String logPrefix) {
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        if ("http".equals(scheme) || "https".equals(scheme)) {
            try {
                String filename = Integer.toHexString(uri.toString().hashCode());
                File cacheLargeIconsDir = new File(WonderPush.getApplicationContext().getCacheDir(), cacheSubfolder);
                cacheLargeIconsDir.mkdirs();
                File cached = new File(cacheLargeIconsDir, filename);
                // TODO handle If-Modified-Since
                if (!cached.exists()) {

                    WonderPush.logDebug(logPrefix + ": Will open URL: " + uri);
                    URLConnection conn = new URL(uri.toString()).openConnection();
                    InputStream is = (InputStream) conn.getContent();
                    WonderPush.logDebug(logPrefix + ": Content-Type: " + conn.getContentType());
                    WonderPush.logDebug(logPrefix + ": Content-Length: " + conn.getContentLength() + " bytes");
                    if (conn.getContentLength() > maxFileSize) {
                        throw new RuntimeException(logPrefix + " file too large (" + conn.getContentLength() + " is over " + maxFileSize + " bytes)");
                    }

                    FileOutputStream outputStream = new FileOutputStream(cached);
                    int read, ttl = 0;
                    byte[] buffer = new byte[2048];
                    while ((read = is.read(buffer)) != -1) {
                        ttl += read;
                        if (ttl > maxFileSize) {
                            throw new RuntimeException(logPrefix + " file too large (max " + maxFileSize + " bytes allowed)");
                        }
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.close();
                    WonderPush.logDebug(logPrefix + ": Finished reading " + ttl + " bytes");

                    expireCache(cacheSubfolder, maxCacheSize);
                }

                return cached;
            } catch (Exception ex) {
                Log.e(WonderPush.TAG, logPrefix + ": Failed to fetch from URI " + uri, ex);
            }
        }
        return null;
    }

    private static void expireCache(String cacheSubfolder, int maxCacheSize) {
        File dir = new File(WonderPush.getApplicationContext().getCacheDir(), cacheSubfolder);
        if (!dir.isDirectory()) return;

        // Sort from most recently modified to least recently modified
        File[] files = dir.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return (int) (rhs.lastModified() - lhs.lastModified());
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

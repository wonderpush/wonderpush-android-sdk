package com.wonderpush.sdk.remoteconfig;

import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
public class RemoteConfigAndroidTest {

    private RemoteConfig fetchedConfig;
    private Throwable fetchError;
    private RemoteConfig storedConfig;
    private Throwable storageError;
    private RemoteConfigFetcher fetcher;
    private RemoteConfigStorage storage;
    private RemoteConfigManager manager;

    @Before
    public void prepare() {
        fetcher = new RemoteConfigFetcher() {
            @Override
            public void fetchRemoteConfig(@Nullable String version, RemoteConfigHandler handler) {
                handler.handle(fetchedConfig, null);
            }
        };
        storage = new RemoteConfigStorage() {
            @Override
            public void storeRemoteConfig(RemoteConfig config, @Nullable ErrorHandler handler) {
                storedConfig = config;
                if (handler != null) handler.handle(storageError);
            }

            @Override
            public void loadRemoteConfigAndHighestDeclaredVersion(RemoteConfigVersionHandler handler) {
                handler.handle(storedConfig, null, storageError);
            }

            @Override
            public void declareVersion(@Nonnull String version, @Nullable ErrorHandler handler) {
                if (handler != null) handler.handle(storageError);
            }
        };

        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        manager = new RemoteConfigManager(fetcher, storage, appContext);
    }



    /**
     * Verify WPRemoteConfigUpdatedNotification happens
     */
    @Test(timeout = 1000)
    public void testNotification() throws ExecutionException, InterruptedException {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Create a manager with a dummy fetcher and storage.
        fetchedConfig = RemoteConfig.with(new JSONObject(), "1");

        CompletableFuture<Void> future = new CompletableFuture<>();

        // Listen to broadcasts.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String remoteConfigString = intent.getStringExtra(Constants.EXTRA_REMOTE_CONFIG);
                assertNotNull(remoteConfigString);

                RemoteConfig remoteConfig = RemoteConfig.fromString(remoteConfigString);
                assertNotNull(remoteConfig);

                assertEquals(remoteConfig.getVersion(), "1");
                assertEquals(remoteConfig.getData().toString(), "{}");

                future.complete(null);
            }
        };
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(appContext);
        mgr.registerReceiver(receiver, new IntentFilter(Constants.INTENT_REMOTE_CONFIG_UPDATED));

        manager.read((RemoteConfig conf, Throwable error) -> {});
        future.get();
        mgr.unregisterReceiver(receiver);
    }

    /**
     * Verify WPRemoteConfigUpdatedNotification doesn't happen when we fetch the same version
     */
    @Test(timeout = 1000)
    public void testNotificationSameVersion() throws ExecutionException, InterruptedException {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Create a manager with a dummy fetcher and storage.
        fetchedConfig = RemoteConfig.with(new JSONObject(), "1");
        storedConfig = fetchedConfig;

        CompletableFuture<Void> future = new CompletableFuture<>();

        // Listen to broadcasts.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                fail();
                future.complete(null);
            }
        };
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(appContext);
        mgr.registerReceiver(receiver, new IntentFilter(Constants.INTENT_REMOTE_CONFIG_UPDATED));

        manager.declareVersion("2");

        // Wait 100ms to let the test fail if a broadcast is sent
        new Thread(() -> {
            try {
                Thread.sleep(100);
                future.complete(null);
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();

        future.get();
        mgr.unregisterReceiver(receiver);
    }

    @Test(timeout = 1000)
    public void testSharedPreferencesStorage() throws ExecutionException, InterruptedException, JSONException {
        String clientId = "unittestsclientid";
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferencesRemoteConfigStorage storage = new SharedPreferencesRemoteConfigStorage(clientId, appContext);

        // Clean sharedPreferences
        String sharedPreferencesFilename = storage.getSharedPreferencesFilename();
        assertNotNull(sharedPreferencesFilename);

        SharedPreferences sharedPref = appContext.getSharedPreferences(
                sharedPreferencesFilename, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        assertTrue(editor.commit());

        CompletableFuture<Void> future = new CompletableFuture<>();

        Date now = DateHelper.now();
        RemoteConfig config = RemoteConfig.with(new JSONObject("{\"toto\": \"titi\"}"), "1", now);

        // Store remote config
        storage.storeRemoteConfig(config, (Throwable error) -> {
            assertNull(error);

            // Declare version 1.2.3
            storage.declareVersion("1.2.3", (Throwable error1) -> {
                assertNull(error1);

                // Declare version 1.0.0
                storage.declareVersion("1.0.0", (Throwable error2) -> {
                    assertNull(error2);

                    // Load
                    storage.loadRemoteConfigAndHighestDeclaredVersion((RemoteConfig conf1, String highestVersion, Throwable error3) -> {
                        assertNull(error3);
                        assertEquals("1.2.3", highestVersion);
                        assertEquals("titi", conf1.getData().optString("toto"));
                        future.complete(null);
                    });
                });
            });
        });
        future.get();
    }

}

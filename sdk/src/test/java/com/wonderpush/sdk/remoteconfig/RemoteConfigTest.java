package com.wonderpush.sdk.remoteconfig;

import com.wonderpush.sdk.SimpleVersion;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class RemoteConfigTest {
    static class MockRemoteConfigFetcher implements RemoteConfigFetcher {

        @Nullable
        public RemoteConfig fetchedConfig;

        @Nullable
        public Throwable error;

        @Nullable
        public String lastRequestedVersion;

        @Nullable
        public Date lastRequestedDate;

        @Override
        public void fetchRemoteConfig(@Nullable String version, RemoteConfigHandler handler) {
            lastRequestedVersion = version;
            lastRequestedDate = DateHelper.now();
            handler.handle(fetchedConfig, error);
        }

    }

    static class MockRemoteConfigStorage implements RemoteConfigStorage {

        @Nullable
        RemoteConfig storedConfig;

        @Nullable
        String storedHighestVersion;

        @Nullable
        Throwable error;


        @Override
        public void storeRemoteConfig(RemoteConfig config, @Nullable ErrorHandler handler) {
            storedConfig = config;
            if (handler != null) handler.handle(error);
        }

        @Override
        public void loadRemoteConfigAndHighestDeclaredVersion(RemoteConfigVersionHandler handler) {
            handler.handle(storedConfig, storedHighestVersion, error);
        }

        @Override
        public void declareVersion(@Nonnull String version, @Nullable ErrorHandler handler) {
            if (storedHighestVersion == null || RemoteConfig.compareVersions(storedHighestVersion, version) == -1) {
                storedHighestVersion = version;
            }
            handler.handle(error);
        }
    }

    static class AsyncRemoteConfigFetcher implements RemoteConfigFetcher {

        @Nullable
        RemoteConfigHandler handler;

        @Nullable
        String lastRequestedVersion;

        @Nullable
        Date lastRequestedDate;

        void resolve(@Nullable RemoteConfig config, @Nullable Throwable error) {
            if (handler != null) handler.handle(config, error);
        }

        @Override
        public void fetchRemoteConfig(@Nullable String version, RemoteConfigHandler handler) {
            lastRequestedDate = DateHelper.now();
            lastRequestedVersion = version;
            this.handler = handler;
        }
    }

    MockRemoteConfigFetcher fetcher;
    MockRemoteConfigStorage storage;
    RemoteConfigManager manager;

    @Before
    public void prepare() {
        fetcher = new MockRemoteConfigFetcher();
        storage = new MockRemoteConfigStorage();
        manager = new RemoteConfigManager(fetcher, storage);
    }

    /**
     * First time use of a manager declareVersion should trigger a download
     */
    @Test
    public void testInitialDeclareVersion() {
        assertNull(fetcher.fetchedConfig);
        assertNull(storage.storedConfig);

        RemoteConfig config = RemoteConfig.with(new JSONObject(), "1.0.0");
        fetcher.fetchedConfig = config;
        manager.declareVersion("1");

        // Declaring a version should trigger a fetch
        assertNotNull(fetcher.lastRequestedDate);
        assertEquals(fetcher.lastRequestedVersion, "1");
        assertSame(storage.storedConfig, config);

        // The manager should have called storage to remember the highest declared version
        assertEquals(storage.storedHighestVersion, "1");
    }

    /**
     * Checking that fetching without prior declareVersion call fetches the config.
     */
    @Test
    public void testInitialRead() throws ExecutionException, InterruptedException {
        assertNull(fetcher.fetchedConfig);
        assertNull(storage.storedConfig);

        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1.0.0");

        CompletableFuture<Void> future = new CompletableFuture<>();

        manager.read((RemoteConfig config, Throwable error) -> {
            assertEquals(config.getVersion(), "1");
            // We don't know what version is out there
            assertNull(fetcher.lastRequestedVersion);
            assertNull(error);
            assertSame(config, fetcher.fetchedConfig);
            assertSame(config, storage.storedConfig);
            future.complete(null);
        });

        // Wait for test async stuff to complete
        future.get();
    }

    /**
     * Ensure we never fetch config more than once every `minimumFetchInterval`,
     *  and we don't consider new versions until `minimumConfigAge` is reached.
     */
    @Test
    public void testRateLimiting() throws ExecutionException, InterruptedException {
        manager.minimumConfigAge = 250;
        manager.minimumFetchInterval = 1000;
        manager.maximumConfigAge = 10000;

        // A brand new config, just fetched
        storage.storedConfig = RemoteConfig.with(new JSONObject(), "1", DateHelper.now());

        // A fetcher ready to serve an even fresher config
        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1.0.1");

        // Yet, we'll get 1.0.0 because we don't fetch before minimumConfigAge.
        manager.read((RemoteConfig config, Throwable error) -> {
            assertEquals(config.getVersion(), "1");
        });
        assertNull(fetcher.lastRequestedDate);

        // Declare a later version
        manager.declareVersion("1.0.1");
        assertEquals(storage.storedHighestVersion, "1.0.1");

        // We won't consider this version until configMaxAge is reached.
        assertNull(fetcher.lastRequestedDate);

        // Try to get the config
        manager.read((RemoteConfig confi, Throwable error) -> {
            // Should still be 1.0.0 because we haven't reached minimumFetchInterval yet
            assertEquals(confi.getVersion(), "1");
            assertNull(fetcher.lastRequestedDate);
        });

        // Wait for the minimumConfigAge and try again, a fetch should happen,
        // just because we declared a higher version.
        long waitTime1 = 2 * manager.minimumConfigAge;
        long waitTime2 = 2 * manager.minimumFetchInterval;

        CompletableFuture<Void> future = new CompletableFuture<>();
        setTimeout(() -> {
            // Meanwhile, nothing should have happened
            assertNull(fetcher.lastRequestedDate);

            // Read
            manager.read((RemoteConfig conf, Throwable error) -> {
                // A fetch just happened
                assertEquals(conf.getVersion(), "1.0.1");
                assertNull(error);
                assertNotNull(fetcher.lastRequestedDate);

                // Declare an even newer version right now, no fetch should happen because minimumFetchInterval still not reached
                // Disclaimer: because the fetchDate of the 1.0.1 version is the same as 1.0.0, it's reached its configMaxAge
                fetcher.lastRequestedDate = null;
                manager.declareVersion("1.0.2");
                assertNull(fetcher.lastRequestedDate);

                // Wait minimumFetchInterval
                setTimeout(() -> {
                    manager.read((RemoteConfig conf1, Throwable error1) -> {
                        // A fetch happened
                        assertNotNull(fetcher.lastRequestedDate);

                        // Finish the test
                        future.complete(null);
                    });
                }, waitTime2);
            });

        }, waitTime1);

        future.get();
    }

    /**
     *  Ensure we don't fetch a new version of the config as long as maximumFetchInterval
     *  is not reached and a new version hasn't been declared.
     */
    @Test
    public void testCaching() throws ExecutionException, InterruptedException {
        manager.minimumConfigAge = 0;
        manager.maximumConfigAge = 2000;
        storage.storedConfig = RemoteConfig.with(new JSONObject(), "1.0.0", DateHelper.now());
        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1.0.1", DateHelper.now());
        CompletableFuture<Void> future = new CompletableFuture<>();
        manager.read((RemoteConfig config, Throwable error) -> {
            assertEquals(config.getVersion(), "1");
            future.complete(null);
        });
        assertNull(fetcher.lastRequestedDate);
        future.get();
    }

    /**
     * Ensure we fetch config when isExpired is true
     */
    @Test(timeout = 1000)
    public void testIsExpired() throws ExecutionException, InterruptedException {
        // Remove rate limiting
        manager.minimumFetchInterval = 0;

        // Make a config that expires in 100ms
        storage.storedConfig = RemoteConfig.with(new JSONObject(), "1.0", DateHelper.now(), 100);
        CompletableFuture<Void> future = new CompletableFuture<>();

        setTimeout(() -> {
            assertTrue(storage.storedConfig.isExpired());
            assertNull(fetcher.lastRequestedDate);
            manager.read((RemoteConfig conf, Throwable error) -> {
                assertNotNull(fetcher.lastRequestedDate);
                future.complete(null);
            });
        }, 101);

        future.get();
    }

    /**
     * Ensure we fetch config when the max fetch interval is reached
     */
    @Test
    public void testMaximumFetchInterval() {
        manager.minimumConfigAge = 100;
        manager.maximumConfigAge = 1000;

        // An expired config
        storage.storedConfig = RemoteConfig.with(new JSONObject(), "1.0.0", new Date(DateHelper.now().getTime() - 1100));

        // A brand new config
        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1.0.1", DateHelper.now());

        manager.read((RemoteConfig conf, Throwable error) -> {
            assertEquals(conf.getVersion(), "1.0.1");
        });

        assertNotNull(fetcher.lastRequestedDate);
    }

    /**
     * Verify we immediately fetch config when minimumFetchInterval is 0 and a new version is declared
     */
    @Test
    public void testZeroMinimumFetchInterval() throws ExecutionException, InterruptedException {
        manager.minimumConfigAge = 0;
        manager.maximumConfigAge = 2000;
        manager.minimumFetchInterval = 0;

        storage.storedConfig = RemoteConfig.with(new JSONObject(), "1.0.0", DateHelper.now());
        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1.0.1", DateHelper.now());
        assertNull(fetcher.lastRequestedDate);

        // Declare new version
        manager.declareVersion("1.0.1");

        // Verify a fetch happened
        assertNotNull(fetcher.lastRequestedDate);

        CompletableFuture<Void> future = new CompletableFuture<>();

        // Verify the version is now 1.0.1
        manager.read((RemoteConfig conf, Throwable error) -> {
            assertEquals(conf.getVersion(), "1.0.1");
            future.complete(null);
        });

        future.get();
    }

    /**
     * Ensure we don't store invalid versions
     */
    @Test
    public void testInvalidVersion() {
        manager.declareVersion("toto");
        AtomicBoolean called = new AtomicBoolean(false);
        storage.loadRemoteConfigAndHighestDeclaredVersion((RemoteConfig config, String highestVersion, Throwable e) -> {
            called.set(true);
            assertNull(highestVersion);
        });
        assertTrue(called.get());
    }

    /**
     * Verify we only fetch when the version goes up.
     */
    @Test
    public void testVersionIncrement() throws ExecutionException, InterruptedException {
        manager.minimumConfigAge = 500;
        manager.minimumFetchInterval = 500;
        manager.maximumConfigAge = 10000; // We don't want to reach this

        // A config that's older than minimumAge
        storage.storedConfig = RemoteConfig.with(new JSONObject(), "1", new Date(DateHelper.now().getTime() - 5000));

        // No fetch yet
        assertNull(fetcher.lastRequestedDate);

        // Declare earlier version
        manager.declareVersion("0.1");

        // Still no fetch because the version is too low
        assertNull(fetcher.lastRequestedDate);

        // Let's declare a later version but download an earlier version
        // (which is possible because of network caches, the API can way a new version is available,
        // yet caches are not up to date yet)
        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "0.1");
        manager.declareVersion("1.0.1");
        assertEquals(storage.storedHighestVersion, "1.0.1");

        // A fetch should have happened
        assertNotNull(fetcher.lastRequestedDate);

        // Yet, when we read, the version should still be 1.0.0 because we fetched version 0.1
        // And no fetch should happen (it's too early)
        fetcher.lastRequestedDate = null;
        manager.read((RemoteConfig conf, Throwable error) -> {
            assertNull(fetcher.lastRequestedDate);
            assertEquals(conf.getVersion(), "1");
        });

        // Wait for minimumFetchInterval, there should be a fetch when we read
        long waitTime = 2 * manager.minimumFetchInterval;
        CompletableFuture<Void> future = new CompletableFuture<>();
        assertNull(fetcher.lastRequestedDate);

        setTimeout(() -> {
            assertNull(fetcher.lastRequestedDate);
            manager.read((RemoteConfig conf, Throwable error) -> {
                // Version is still 1
                assertEquals(conf.getVersion(), "1");

                // We should have tried to fetch though
                assertNotNull(fetcher.lastRequestedDate);

                future.complete(null);
            });
        }, waitTime);

        future.get();
    }

    /**
     * Checks that the highest declared version is well maintained between what is declared and what is fetched.
     */
    @Test
    public void testDeclaredVersion() {
        manager.minimumConfigAge = 0;
        manager.minimumFetchInterval = 0;
        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1");
        manager.declareVersion("1.0.1");
        assertEquals(storage.storedHighestVersion, "1.0.1");

        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1.0.3");
        manager.declareVersion("1.0.2");

        assertEquals(storage.storedHighestVersion, "1.0.3");
    }

    /**
     * Verify version comparison
     */
    @Test
    public void testVersionComparison() {
        assertEquals(0, RemoteConfig.compareVersions("1.0", "1.0.0"));
        assertEquals(-1, RemoteConfig.compareVersions("1.0.0", "1.0.1"));
        assertEquals(1, RemoteConfig.compareVersions("1.0.0", "0.9"));
        // With "v" prefix
        assertEquals(0, RemoteConfig.compareVersions("v1.0", "v1.0.0"));
        assertEquals(-1, RemoteConfig.compareVersions("v1.0.0", "v1.0.1"));
        assertEquals(1, RemoteConfig.compareVersions("v1.0.0", "v0.9"));
        // With or without "v" prefix
        assertEquals(0, RemoteConfig.compareVersions("1.0", "v1.0.0"));
        assertEquals(-1, RemoteConfig.compareVersions("1.0.0", "v1.0.1"));
        assertEquals(1, RemoteConfig.compareVersions("1.0.0", "v0.9"));
        // With invalid version
        assertEquals(1, RemoteConfig.compareVersions("1.0", "z"));
        assertEquals(1, RemoteConfig.compareVersions("1.0", "_"));
        assertEquals(1, RemoteConfig.compareVersions("1.0", "/"));
        assertEquals(1, RemoteConfig.compareVersions("1.0", "!"));
        assertEquals(1, RemoteConfig.compareVersions("1.0", "."));
        assertEquals(-1, RemoteConfig.compareVersions("z", "1.0"));
        assertEquals(-1, RemoteConfig.compareVersions("_", "1.0"));
        assertEquals(-1, RemoteConfig.compareVersions("/", "1.0"));
        assertEquals(-1, RemoteConfig.compareVersions("!", "1.0"));
        assertEquals(-1, RemoteConfig.compareVersions(".", "1.0"));
        assertEquals(0, RemoteConfig.compareVersions("z", "/"));
        assertEquals(0, RemoteConfig.compareVersions("_", "/"));
        assertEquals(0, RemoteConfig.compareVersions("/", "/"));
        assertEquals(0, RemoteConfig.compareVersions("!", "/"));
        assertEquals(0, RemoteConfig.compareVersions(".", "/"));
    }

    /**
     * When the same version of the config is fetched a second time, its fetch date should be updated.
     */
    @Test
    public void testUpdateConfigAge() throws ExecutionException, InterruptedException {
        manager.minimumFetchInterval = 0;
        manager.minimumConfigAge = 0;
        manager.maximumConfigAge = 10000;

        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1");
        CompletableFuture<Void> future = new CompletableFuture<>();

        long timeToWait = 500;
        manager.read((RemoteConfig conf, Throwable error) -> {
            Date fetchDate = conf.getFetchDate();

            // Config should be newer than 100ms
            assertNotNull(fetchDate);
            assertTrue(DateHelper.now().getTime() - fetchDate.getTime() < 100);

            // Wait 500ms
            setTimeout(() -> {
                fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1", DateHelper.now());
                manager.declareVersion("1.0.1");
                manager.read((RemoteConfig conf1, Throwable error1) -> {

                    // Config fetch date should have been updated even though we've fetched the same version again
                    assertNotNull(conf1.getFetchDate());
                    assertTrue(conf1.getFetchDate().getTime() - fetchDate.getTime() > timeToWait);

                    // Finish test
                    future.complete(null);
                });
            }, timeToWait);
        });

        future.get();
    }

    /**
     * When a version is declared, that is the same as the current stored config, its fetch date should be updated.
     */
    @Test(timeout = 1000)
    public void testUpdateConfigAge2() throws ExecutionException, InterruptedException {
        manager.minimumFetchInterval = 0;
        manager.minimumConfigAge = 0;
        manager.maximumConfigAge = 10000;

        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1");
        CompletableFuture<Void> future = new CompletableFuture<>();

        long timeToWait = 500;
        manager.read((RemoteConfig conf, Throwable error) -> {
            Date fetchDate = conf.getFetchDate();

            // Config should be newer than 100ms
            assertNotNull(fetchDate);
            assertTrue(DateHelper.now().getTime() - fetchDate.getTime() < 100);

            // Wait 500ms
            setTimeout(() -> {
                fetcher.lastRequestedDate = null;
                manager.declareVersion("1");
                assertNull(fetcher.lastRequestedDate);
                manager.read((RemoteConfig conf1, Throwable error1) -> {

                    assertNull(fetcher.lastRequestedDate);
                    // Config fetch date should have been updated even though we haven't fetched any new config
                    assertNotNull(conf1.getFetchDate());
                    long toto = conf1.getFetchDate().getTime() - fetchDate.getTime();
                    assertTrue(conf1.getFetchDate().getTime() - fetchDate.getTime() > timeToWait);

                    // Finish test
                    future.complete(null);
                });
            }, timeToWait);
        });

        future.get();
    }

    /**
     * When a version is declared, that is earlier as the stored config's version, its fetch date should NOT be updated.
     * This is the opposite of testUpdateConfigAge2.
     */
    @Test(timeout = 1000)
    public void testUpdateConfigAge3() throws ExecutionException, InterruptedException {
        manager.minimumFetchInterval = 0;
        manager.minimumConfigAge = 0;
        manager.maximumConfigAge = 10000;

        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1");
        Date fetchDate = fetcher.fetchedConfig.getFetchDate();
        CompletableFuture<Void> future = new CompletableFuture<>();

        long timeToWait = 500;
        manager.read((RemoteConfig conf, Throwable error) -> {

            // Config should be newer than 100ms
            assertNotNull(fetchDate);
            assertTrue(DateHelper.now().getTime() - fetchDate.getTime() < 100);

            // Wait 500ms
            setTimeout(() -> {
                fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1", DateHelper.now());
                manager.declareVersion("0.5");
                manager.read((RemoteConfig conf1, Throwable error1) -> {

                    // Config fetch date should have been updated even though we haven't fetched any new config
                    assertNotNull(conf1.getFetchDate());
                    assertEquals(conf1.getFetchDate(), fetchDate);

                    // Finish test
                    future.complete(null);
                });
            }, timeToWait);
        });

        future.get();
    }

    @Test
    public void testSerialize() {
        String fileName = "remoteconfig/remote-config-example.json";
        ClassLoader classLoader = this.getClass().getClassLoader();

        JSONObject configJSON = null;
        try {
            configJSON = new JSONObject(new String(Files.readAllBytes(new File(classLoader.getResource(fileName).getFile()).toPath())));
        } catch (Exception e) {
            fail();
        }

        assertNotNull(configJSON);
        Date fetchDate = DateHelper.now();
        assertTrue(configJSON.has("version"));
        String version = configJSON.optString("version", Long.toString(configJSON.optLong("version", 0))  );
        long maxAge = configJSON.optLong("maxAge");
        assertEquals(0, new SimpleVersion(version).compareTo(new SimpleVersion("1.0")));
        assertEquals(maxAge, 123456);

        RemoteConfig remoteConfig = RemoteConfig.with(configJSON, version, fetchDate, maxAge);
        String encoded = remoteConfig.toString();
        RemoteConfig decoded = RemoteConfig.fromString(encoded);

        assertNotNull(decoded);
        assertEquals(remoteConfig.getVersion(), decoded.getVersion());
        assertEquals(remoteConfig.getFetchDate(), decoded.getFetchDate());
        assertEquals(remoteConfig.getMaxAge(), decoded.getMaxAge());
        assertEquals(123456, decoded.getMaxAge());
        assertEquals(remoteConfig.getData().toString(), remoteConfig.getData().toString());
    }

    /**
     * Ensures that when we request the config and it is currently fetching, we wait for the result.
     */
    @Test
    public void testConcurrentFetches() throws ExecutionException, InterruptedException {
        // The async fetcher lets us call the completion handler ourselves.
        AsyncRemoteConfigFetcher fetcher = new AsyncRemoteConfigFetcher();
        manager.remoteConfigFetcher = fetcher;

        CompletableFuture<Void> future = new CompletableFuture<>();
        ArrayList<String> count = new ArrayList<>();

        // A runnable that counts and completes the future when it's been called twice
        Runnable fulfill = () -> {
            synchronized (count) {
                count.add("fulfill");
                if (count.size() > 1) future.complete(null);
            }
        };

        RemoteConfig resultConfig = RemoteConfig.with(new JSONObject(), "1");

        // Read
        manager.read((RemoteConfig config, Throwable error) -> {
            assertNotNull(config);
            assertSame(config, resultConfig);
            fulfill.run();
        });

        // A new fetch should have been triggered
        RemoteConfigHandler handler = fetcher.handler;
        Date lastRequestedDate = fetcher.lastRequestedDate;
        assertNotNull(lastRequestedDate);
        assertNotNull(handler);

        // Read again before result comes
        manager.read((RemoteConfig config, Throwable error) -> {
            assertNotNull(config);
            assertSame(config, resultConfig);
            fulfill.run();
        });

        // A new fetch hasn't been requested
        assertSame(handler, fetcher.handler);
        assertSame(lastRequestedDate, fetcher.lastRequestedDate);

        // Resolve
        fetcher.resolve(resultConfig, null);

        future.get();
    }

    private void setTimeout(Runnable runnable, long delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();
    }

    @Test
    public void testFromString() throws JSONException {
        RemoteConfig config;

        JSONObject json;

        // missing version
        json = new JSONObject();
        config = RemoteConfig.fromString(json.toString());

        assertNull(config);

        config = RemoteConfig.fromString("{\"version\":\"1.0.1\"}");
        assertEquals(config.getVersion(), "1.0.1");
        assertNotNull(config.getData());

        config = RemoteConfig.fromString("{\"version\":\"1.0.1\", \"maxAge\":123456, \"fetchDate\":1600785789000}");
        assertEquals(config.getMaxAge(), 123456);
        assertEquals(config.getFetchDate(), new Date(1600785789000l));
        assertNotNull(config.getData());

        config = RemoteConfig.fromString("{\"version\":\"1.0.1\", \"minAge\":123456}");
        assertEquals(config.getMinAge(), 123456);
    }

    /**
     * checks that minAge specified at config level is effective.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testConfigMinAge() throws ExecutionException, InterruptedException {
        manager.minimumConfigAge = 0;
        manager.minimumFetchInterval = 1000;
        manager.maximumConfigAge = 10000;

        // A brand new config, just fetched
        storage.storedConfig = RemoteConfig.with(new JSONObject(), "1", DateHelper.now(), 0, 100);

        // A fetcher ready to serve an even fresher config
        fetcher.fetchedConfig = RemoteConfig.with(new JSONObject(), "1.0.1");

        // Yet, we'll get 1.0.0 because we don't fetch before minimumConfigAge.
        manager.read((RemoteConfig config, Throwable error) -> {
            assertEquals(config.getVersion(), "1");
        });
        assertNull(fetcher.lastRequestedDate);

        // Declare a later version
        manager.declareVersion("1.0.1");
        assertEquals(storage.storedHighestVersion, "1.0.1");

        // We won't consider this version until minAge is reached.
        assertNull(fetcher.lastRequestedDate);

        // Try to get the config
        manager.read((RemoteConfig confi, Throwable error) -> {
            // Should still be 1.0.0 because we haven't reached minAge yet
            assertEquals(confi.getVersion(), "1");
            assertNull(fetcher.lastRequestedDate);
        });

        // Wait for the minimumConfigAge and try again, a fetch should happen,
        // just because we declared a higher version.
        long waitTime1 = 2 * storage.storedConfig.getMinAge();

        CompletableFuture<Void> future = new CompletableFuture<>();
        setTimeout(() -> {
            // Meanwhile, nothing should have happened
            assertNull(fetcher.lastRequestedDate);

            // Read
            manager.read((RemoteConfig conf, Throwable error) -> {
                // A fetch just happened
                assertEquals(conf.getVersion(), "1.0.1");
                assertNull(error);
                assertNotNull(fetcher.lastRequestedDate);
                future.complete(null);

            });

        }, waitTime1);

        future.get();
    }

    /**
     * Defines behavior when a 404 on the config occurs.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void test404() throws ExecutionException, InterruptedException {
        fetcher.error = new Exception("404");
        fetcher.fetchedConfig = null;
        manager.minimumFetchInterval = 100;

        CompletableFuture<Void> future = new CompletableFuture<>();

        manager.read((RemoteConfig config1, Throwable error1) -> {
            // no config, get an error, updated requested date
            assertNull(config1);
            assertEquals(error1.getMessage(), "404");
            assertNotNull(fetcher.lastRequestedDate);

            fetcher.lastRequestedDate = null;

            // Read right away
            manager.read((RemoteConfig config2, Throwable error2) -> {
                assertNull(config2);
                // We haven't made a fetch, so no error, and no requested date
                assertNull(error2);
                assertNull(fetcher.lastRequestedDate);

                // Wait for minimumFetchInterval
                setTimeout(() -> {
                    manager.read((RemoteConfig config3, Throwable error3) -> {
                        assertNull(config3);
                        // We've made a fetch, so error and lastRequestedDate are not nil
                        assertNotNull(error3);
                        assertNotNull(fetcher.lastRequestedDate);
                        future.complete(null);
                    });
                }, 100);
            });
        });
        future.get();
    }

    /**
     * Verifies that when a fetch error occurs, we serve a previously fetched config.
     */
    @Test
    public void testFetchError() {
        // Fetch as often as we like
        manager.minimumFetchInterval = 0;
        manager.minimumConfigAge = 0;

        // Configure a previously fetched config
        final RemoteConfig storedConfig = RemoteConfig.with(new JSONObject(), "1.0");
        storage.storedConfig = storedConfig;
        // Configure fetch error
        fetcher.error = new Exception("fetch error");

        // Declare higher version
        manager.declareVersion("1.1");

        // read
        manager.read((RemoteConfig config, Throwable error) -> {
            // We're serving the previous config
            assertSame(storedConfig, config);
            // We're also reporting the error
            assertEquals("fetch error", error.getMessage());
        });
    }

    /**
     * Checks that when a particular config entry is present, no new configuration will be fetched if we declare a new version
     */
    @Test
    public void testDisableFetchNewVersion() throws JSONException {

        // Fetch as often as we like
        manager.minimumConfigAge = 0;
        manager.minimumFetchInterval = 0;

        // A config has already been fetched, that forbids further fetching via the DISABLE_FETCH_KEY
        JSONObject data = new JSONObject();
        data.put(Constants.DISABLE_FETCH_KEY, true);
        final RemoteConfig storedConfig = RemoteConfig.with(data, "1");
        storage.storedConfig = storedConfig;

        manager.read((RemoteConfig config, Throwable error) -> {
            assertSame(storedConfig, config);
            assertNull(fetcher.lastRequestedDate);
        });

        // Declare new version
        manager.declareVersion("2");

        // No fetch should have happened
        manager.read((RemoteConfig config, Throwable error) -> {
            assertSame(storedConfig, config);
            assertNull(fetcher.lastRequestedDate);
        });
    }

    /**
     * Checks that when a particular config entry is present, no new configuration will be fetched even after it expires
     */
    @Test
    public void testDisableFetchExpired() throws JSONException, ExecutionException, InterruptedException {

        // Fetch as often as we like
        manager.minimumConfigAge = 0;
        manager.minimumFetchInterval = 0;
        manager.maximumConfigAge = 100;

        // A config has already been fetched, that forbids further fetching via the DISABLE_FETCH_KEY
        JSONObject data = new JSONObject();
        data.put(Constants.DISABLE_FETCH_KEY, true);
        final RemoteConfig storedConfig = RemoteConfig.with(data, "1", new Date(), 100);
        storage.storedConfig = storedConfig;

        manager.read((RemoteConfig config, Throwable error) -> {
            assertSame(storedConfig, config);
            assertNull(fetcher.lastRequestedDate);
        });

        CompletableFuture<Void> future = new CompletableFuture<>();

        setTimeout(() -> {
            // No fetch should have happened
            manager.read((RemoteConfig config, Throwable error) -> {
                assertSame(storedConfig, config);
                assertNull(fetcher.lastRequestedDate);
                future.complete(null);
            });

        }, 150);

        future.get();
    }
}

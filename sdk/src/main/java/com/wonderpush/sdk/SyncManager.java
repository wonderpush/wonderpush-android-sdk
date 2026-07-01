package com.wonderpush.sdk;

import com.wonderpush.sdk.remoteconfig.RemoteConfig;
import com.wonderpush.sdk.remoteconfig.RemoteConfigManager;

/**
 * SDK-init lifecycle manager for the sdk-sync channel. Ported from wonderpush-ios-sdk
 * {@code WPSyncManager}. Lazily builds the stack on first {@link #refresh}, and gates the whole
 * feature on the {@code syncEnabled} remote-config flag (default off -> inert). Only when enabled is
 * the request observer installed into {@link SyncHook}, so {@link BaseApiClient} stays a no-op until
 * the server opts a project in.
 */
class SyncManager {

    private static SyncManager sInstance;

    static synchronized SyncManager getInstance() {
        if (sInstance == null) sInstance = new SyncManager();
        return sInstance;
    }

    private Sync sync;
    private SyncKnobs cachedKnobs = SyncKnobs.defaultKnobs();
    private boolean stackBuilt;
    private boolean installed;

    /**
     * Refresh knobs + the enable gate from remote config, building the stack if needed.
     *
     * @param remoteConfigManager source of the {@code sync*} knobs and {@code syncEnabled} flag
     * @param identifiersProvider supplies the current userId/deviceId/installationId/visitorId
     * @param sender              issues the explicit-fetch GET (wraps {@link ApiClient})
     */
    void refresh(RemoteConfigManager remoteConfigManager,
                 Sync.IdentifiersProvider identifiersProvider,
                 SyncApiRequestSender sender) {
        buildStackIfNeeded(identifiersProvider, sender);
        remoteConfigManager.read((RemoteConfig config, Throwable error) -> {
            RemoteConfig effective = error != null ? null : config;
            cachedKnobs = SyncKnobs.mergeKnobs(SyncKnobs.defaultKnobs(),
                    effective != null ? effective.getData() : null);
            // Master gate: only go live when the server explicitly enables sync (default off -> inert).
            boolean enabled = effective != null && effective.getData() != null
                    && effective.getData().optBoolean("syncEnabled");
            if (enabled && !installed) {
                SyncHook.installObserver(sync);
                installed = true;
            } else if (!enabled && installed) {
                SyncHook.installObserver(null);
                installed = false;
            }
        });
    }

    /** The synced payload for a source under the current profile. nil-safe before the stack is built. */
    Object dataForSource(String source) {
        return sync != null ? sync.dataForSource(source) : null;
    }

    /** Register a listener for source-data changes (used by the in-app engine for "popups"). */
    void addSourceDataChangeListener(Sync.SourceDataChangeListener listener) {
        if (sync != null) sync.addSourceDataChangeListener(listener);
    }

    private synchronized void buildStackIfNeeded(Sync.IdentifiersProvider identifiersProvider,
                                                 SyncApiRequestSender sender) {
        if (stackBuilt) return;
        stackBuilt = true;

        SyncStateStore store = SyncStateStore.defaultStore();
        SyncApiTransport transport = new SyncApiTransport(sender);
        SyncFetcher fetcher = new SyncFetcher(store, transport);

        Sync s = new Sync(store, fetcher);
        s.identifiersProvider = identifiersProvider;
        s.knobsProvider = () -> cachedKnobs != null ? cachedKnobs : SyncKnobs.defaultKnobs();

        s.registerSource("contact", new SyncContactSource());
        s.registerSource("popups", new SyncPopupsSource());
        this.sync = s;
    }
}

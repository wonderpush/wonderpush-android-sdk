package com.wonderpush.sdk.inappmessaging.internal.injection.modules;

import com.wonderpush.sdk.WonderPush;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class InternalEventTrackerModule {
    private final WonderPush.InternalEventTracker internalEventTracker;

    public InternalEventTrackerModule(WonderPush.InternalEventTracker internalEventTracker) {
        this.internalEventTracker = internalEventTracker;
    }

    @Provides
    @Singleton
    public WonderPush.InternalEventTracker providesInternalEventTracker() {
        return internalEventTracker;
    }

}

package com.wonderpush.sdk.inappmessaging.internal.injection.modules;

import com.wonderpush.sdk.InternalEventTracker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class InternalEventTrackerModule {
    private final InternalEventTracker internalEventTracker;

    public InternalEventTrackerModule(InternalEventTracker internalEventTracker) {
        this.internalEventTracker = internalEventTracker;
    }

    @Provides
    @Singleton
    public InternalEventTracker providesInternalEventTracker() {
        return internalEventTracker;
    }

}

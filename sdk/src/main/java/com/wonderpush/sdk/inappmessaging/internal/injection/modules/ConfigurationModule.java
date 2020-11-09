package com.wonderpush.sdk.inappmessaging.internal.injection.modules;

import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import dagger.Module;
import dagger.Provides;

@Module
public class ConfigurationModule {
    private static InAppMessaging.InAppMessagingDelegate instance;
    public static void setInstance(InAppMessaging.InAppMessagingDelegate instance) {
        ConfigurationModule.instance = instance;
    }

    public static InAppMessaging.InAppMessagingDelegate getInstance() {
        return instance;
    }

    @Provides
    public InAppMessaging.InAppMessagingDelegate get() {
        return instance;
    }
}

package com.wonderpush.sdk.inappmessaging.internal.injection.modules;

import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessagingScope;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class ConfigurationModule {
    private static InAppMessaging.InAppMessagingConfiguration instance;
    public static void setInstance(InAppMessaging.InAppMessagingConfiguration instance) {
        ConfigurationModule.instance = instance;
    }

    public static InAppMessaging.InAppMessagingConfiguration getInstance() {
        return instance;
    }

    @Provides
    public InAppMessaging.InAppMessagingConfiguration get() {
        return instance;
    }
}

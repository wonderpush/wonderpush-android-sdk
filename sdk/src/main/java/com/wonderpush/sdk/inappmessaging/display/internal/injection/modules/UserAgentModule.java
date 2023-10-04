package com.wonderpush.sdk.inappmessaging.display.internal.injection.modules;

import android.app.Application;

import com.wonderpush.sdk.UserAgentProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Bindings for {@link Application}
 *
 * @hide
 */
@Module
public class UserAgentModule {
  private final UserAgentProvider userAgentProvider;

  public UserAgentModule(UserAgentProvider userAgentProvider) {
    this.userAgentProvider = userAgentProvider;
  }

  @Provides
  @Singleton
  public UserAgentProvider providesUserAgentProvider() {
    return userAgentProvider;
  }
}

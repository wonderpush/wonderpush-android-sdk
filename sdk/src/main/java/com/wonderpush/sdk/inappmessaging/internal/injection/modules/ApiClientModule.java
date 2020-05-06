// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.wonderpush.sdk.inappmessaging.internal.injection.modules;

import android.app.Application;

import com.wonderpush.sdk.inappmessaging.internal.ApiClient;
import com.wonderpush.sdk.inappmessaging.internal.CampaignCacheClient;
import com.wonderpush.sdk.inappmessaging.internal.ProviderInstaller;
import com.wonderpush.sdk.inappmessaging.internal.SharedPreferencesUtils;
import com.wonderpush.sdk.inappmessaging.internal.TestDeviceHelper;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.internal.time.Clock;

import dagger.Module;
import dagger.Provides;

/**
 * Provider for ApiClient
 *
 * @hide
 */
@Module
public class ApiClientModule {
  private final Application application;
  private final Clock clock;

  public ApiClientModule(
          Application application, Clock clock) {
    this.application = application;
    this.clock = clock;
  }

  @Provides
  SharedPreferencesUtils providesSharedPreferencesUtils() {
    return new SharedPreferencesUtils(application);
  }

  @Provides
  TestDeviceHelper providesTestDeviceHelper(SharedPreferencesUtils sharedPreferencesUtils) {
    return new TestDeviceHelper(sharedPreferencesUtils);
  }

  @Provides
  @InAppMessagingScope
  ApiClient providesApiClient(
      Application application,
      CampaignCacheClient campaignCacheClient,
      ProviderInstaller providerInstaller) {
    return new ApiClient(
        application,
        clock,
        campaignCacheClient,
        providerInstaller);
  }
}

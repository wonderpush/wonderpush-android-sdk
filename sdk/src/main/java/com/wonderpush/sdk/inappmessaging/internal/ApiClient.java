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

package com.wonderpush.sdk.inappmessaging.internal;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.internal.time.Clock;
import com.wonderpush.sdk.inappmessaging.model.CampaignImpressionList;
import com.wonderpush.sdk.inappmessaging.model.FetchEligibleCampaignsResponse;

import java.util.concurrent.TimeUnit;

/**
 * Interface to speak to the iam backend
 *
 * @hide
 */
@InAppMessagingScope
public class ApiClient {

  private static final String DATA_COLLECTION_DISABLED_ERROR =
      "Automatic data collection is disabled, not attempting campaign fetch from service.";
  private static final String FETCHING_CAMPAIGN_MESSAGE = "Fetching campaigns from service.";

  private final Application application;
  private final Clock clock;
  private final ProviderInstaller providerInstaller;
  private final CampaignCacheClient campaignCacheClient;
  public ApiClient(
      Application application,
      Clock clock,
      CampaignCacheClient campaignCacheClient,
      ProviderInstaller providerInstaller) {
    this.application = application;
    this.clock = clock;
    this.providerInstaller = providerInstaller;
    this.campaignCacheClient = campaignCacheClient;
  }

  Task<FetchEligibleCampaignsResponse> getIams(CampaignImpressionList impressionList) {
    Logging.logi(FETCHING_CAMPAIGN_MESSAGE);
    providerInstaller.install();
    FetchEligibleCampaignsResponse response = this.campaignCacheClient.get().blockingGet();
    if (response == null) response = new FetchEligibleCampaignsResponse();
    return Tasks.forResult(withCacheExpirationSafeguards(response));
  }

  private FetchEligibleCampaignsResponse withCacheExpirationSafeguards(
      FetchEligibleCampaignsResponse resp) {
    if (resp.getExpirationEpochTimestampMillis() < clock.now() + TimeUnit.MINUTES.toMillis(1)
        || resp.getExpirationEpochTimestampMillis() > clock.now() + TimeUnit.DAYS.toMillis(3)) {
      // we default to minimum 1 day if the expiration passed from the service is less than 1 minute
      resp.setExpirationEpochTimestampMillis(clock.now() + TimeUnit.DAYS.toMillis(1));
    }

    return resp;
  }

  @Nullable
  private String getVersionName() {
    try {
      PackageInfo pInfo =
          application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
      return pInfo.versionName;
    } catch (NameNotFoundException e) {
      Logging.loge("Error finding versionName : " + e.getMessage());
    }
    return null;
  }
}

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

import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.AppForeground;
import com.wonderpush.sdk.inappmessaging.internal.time.Clock;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.ratelimiter.RateLimit;

import javax.inject.Inject;

public class DisplayCallbacksFactory {

  private final ImpressionStorageClient impressionStorageClient;
  private final Clock clock;
  private final RateLimit appForegroundRateLimit;
  private final MetricsLoggerClient metricsLoggerClient;

  @Inject
  public DisplayCallbacksFactory(
      ImpressionStorageClient impressionStorageClient,
      Clock clock,
      @AppForeground RateLimit appForegroundRateLimit,
      MetricsLoggerClient metricsLoggerClient) {
    this.impressionStorageClient = impressionStorageClient;
    this.clock = clock;
    this.appForegroundRateLimit = appForegroundRateLimit;
    this.metricsLoggerClient = metricsLoggerClient;
  }

  public InAppMessagingDisplayCallbacks generateDisplayCallback(
          InAppMessage inAppMessage, String triggeringEvent) {

    return new DisplayCallbacksImpl(
        impressionStorageClient,
        clock,
        appForegroundRateLimit,
        metricsLoggerClient,
        inAppMessage,
        triggeringEvent);
  }
}

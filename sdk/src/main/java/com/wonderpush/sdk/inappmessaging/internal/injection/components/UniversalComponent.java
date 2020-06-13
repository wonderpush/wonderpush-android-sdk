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

package com.wonderpush.sdk.inappmessaging.internal.injection.components;

import android.app.Application;

import com.wonderpush.sdk.InternalEventTracker;
import com.wonderpush.sdk.inappmessaging.internal.AnalyticsEventsManager;
import com.wonderpush.sdk.inappmessaging.internal.CampaignCacheClient;
import com.wonderpush.sdk.inappmessaging.internal.DeveloperListenerManager;
import com.wonderpush.sdk.inappmessaging.internal.ImpressionStorageClient;
import com.wonderpush.sdk.inappmessaging.internal.ProgramaticContextualTriggers;
import com.wonderpush.sdk.inappmessaging.internal.ProviderInstaller;
import com.wonderpush.sdk.inappmessaging.internal.RateLimiterClient;
import com.wonderpush.sdk.inappmessaging.internal.Schedulers;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.AnalyticsEventsModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.ApplicationModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.ForegroundFlowableModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.InternalEventTrackerModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.ProgrammaticContextualTriggerFlowableModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.ProtoStorageClientModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.RateLimitModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.SchedulerModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.SystemClockModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.AnalyticsListener;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.AppForeground;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.ProgrammaticTrigger;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.internal.time.Clock;
import com.wonderpush.sdk.inappmessaging.model.ProtoMarshallerClient;
import com.wonderpush.sdk.inappmessaging.model.RateLimit;

import javax.inject.Singleton;

import dagger.Component;
import io.reactivex.flowables.ConnectableFlowable;

/**
 * A single Network component is shared by all components in the {@link
 * InAppMessagingScope}
 *
 * @hide
 */
@Singleton
@Component(
    modules = {
      SchedulerModule.class,
      ApplicationModule.class,
      InternalEventTrackerModule.class,
      ForegroundFlowableModule.class,
      ProgrammaticContextualTriggerFlowableModule.class,
      AnalyticsEventsModule.class,
      ProtoStorageClientModule.class,
      SystemClockModule.class,
      RateLimitModule.class
    })
public interface UniversalComponent {
  ProviderInstaller probiderInstaller();

  Schedulers schedulers();

  @AppForeground
  ConnectableFlowable<String> appForegroundEventFlowable();

  @ProgrammaticTrigger
  ConnectableFlowable<String> programmaticContextualTriggerFlowable();

  @ProgrammaticTrigger
  ProgramaticContextualTriggers programmaticContextualTriggers();

  @AnalyticsListener
  ConnectableFlowable<String> analyticsEventsFlowable();

  AnalyticsEventsManager analyticsEventsManager();

  CampaignCacheClient campaignCacheClient();

  ImpressionStorageClient impressionStorageClient();

  Clock clock();

  ProtoMarshallerClient protoMarshallerClient();

  RateLimiterClient rateLimiterClient();

  Application application();

  InternalEventTracker internalEventTracker();

  @AppForeground
  RateLimit appForegroundRateLimit();

  DeveloperListenerManager developerListenerManager();
}

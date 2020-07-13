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

import com.wonderpush.sdk.InternalEventTracker;
import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.internal.DeveloperListenerManager;
import com.wonderpush.sdk.inappmessaging.internal.MetricsLoggerClient;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;

import dagger.Module;
import dagger.Provides;

/**
 * Bindings for engagementMetrics
 *
 * @hide
 */
@Module
public class TransportClientModule {
  private static final String TRANSPORT_NAME = "731";

  @Provides
  @InAppMessagingScope
  static MetricsLoggerClient providesApiClient(DeveloperListenerManager developerListenerManager, InternalEventTracker internalEventTracker, InAppMessaging.InAppMessagingConfiguration configuration) {
    return new MetricsLoggerClient(developerListenerManager, internalEventTracker, configuration);
  }
}

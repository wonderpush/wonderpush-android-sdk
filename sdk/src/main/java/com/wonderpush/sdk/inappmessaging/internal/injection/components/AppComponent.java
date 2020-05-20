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

import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.internal.DisplayCallbacksFactory;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.ConfigurationModule;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;

import dagger.Component;

/**
 * Dagger component to create InAppMessaging Objects.
 *
 * @hide
 */
@InAppMessagingScope
@Component(
    dependencies = {UniversalComponent.class},
    modules = {ConfigurationModule.class})
public interface AppComponent {
  InAppMessaging providesInAppMessaging();

  DisplayCallbacksFactory displayCallbacksFactory();

  @Component.Builder
  interface Builder {

    Builder configurationModule(ConfigurationModule module);

    Builder universalComponent(UniversalComponent component);

    AppComponent build();
  }
}

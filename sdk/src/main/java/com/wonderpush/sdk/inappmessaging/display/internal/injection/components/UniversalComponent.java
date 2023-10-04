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

package com.wonderpush.sdk.inappmessaging.display.internal.injection.components;

import android.app.Application;

import com.wonderpush.sdk.UserAgentProvider;
import com.wonderpush.sdk.inappmessaging.display.internal.BindingWrapperFactory;
import com.wonderpush.sdk.inappmessaging.display.internal.IamWindowManager;
import com.wonderpush.sdk.inappmessaging.display.internal.InAppMessageLayoutConfig;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.modules.ApplicationModule;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.modules.InflaterConfigModule;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.modules.UserAgentModule;

import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Component;

/** @hide */
@Singleton
@Component(modules = {ApplicationModule.class, InflaterConfigModule.class, UserAgentModule.class})
public interface UniversalComponent {
  Application providesApplication();

  IamWindowManager iamWindowManager();

  BindingWrapperFactory inflaterClient();

  Map<String, Provider<InAppMessageLayoutConfig>> myKeyStringMap();

  UserAgentProvider providesUserAgent();
}

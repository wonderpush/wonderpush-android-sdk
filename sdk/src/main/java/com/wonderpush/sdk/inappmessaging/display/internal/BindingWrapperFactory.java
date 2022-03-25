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

package com.wonderpush.sdk.inappmessaging.display.internal;

import android.app.Activity;

import android.view.Gravity;
import com.wonderpush.sdk.inappmessaging.display.internal.bindingwrappers.BindingWrapper;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.components.DaggerInAppMessageComponent;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.components.InAppMessageComponent;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.modules.InflaterModule;
import com.wonderpush.sdk.inappmessaging.model.BannerMessage;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;

import javax.inject.Inject;
import javax.inject.Singleton;

/** @hide */
@Singleton
public class BindingWrapperFactory {

  @Inject
  BindingWrapperFactory() {
  }

  public BindingWrapper createImageBindingWrapper(
          Activity activity, InAppMessageLayoutConfig config, InAppMessage inAppMessage) {
    InAppMessageComponent inAppMessageComponent =
        DaggerInAppMessageComponent.builder()
            .inflaterModule(new InflaterModule(inAppMessage, config, activity))
            .build();
    return inAppMessageComponent.imageBindingWrapper();
  }

  public BindingWrapper createWebViewBindingWrapper(
          Activity activity, InAppMessageLayoutConfig config, InAppMessage inAppMessage) {
    InAppMessageComponent inAppMessageComponent =
            DaggerInAppMessageComponent.builder()
                    .inflaterModule(new InflaterModule(inAppMessage, config, activity))
                    .build();
    return inAppMessageComponent.webViewBindingWrapper();
  }

  public BindingWrapper createModalBindingWrapper(
          Activity activity, InAppMessageLayoutConfig config, InAppMessage inAppMessage) {
    InAppMessageComponent inAppMessageComponent =
        DaggerInAppMessageComponent.builder()
            .inflaterModule(new InflaterModule(inAppMessage, config, activity))
            .build();
    return inAppMessageComponent.modalBindingWrapper();
  }

  public BindingWrapper createBannerBindingWrapper(
          Activity activity, InAppMessageLayoutConfig config, InAppMessage inAppMessage) {
    InAppMessageLayoutConfig updatedConfig = config;
    if (inAppMessage instanceof BannerMessage && ((BannerMessage)inAppMessage).getBannerPosition().equals(InAppMessage.BannerPosition.BOTTOM)) {
      try {
        updatedConfig = new InAppMessageLayoutConfig.Builder(config).setViewWindowGravity(Gravity.BOTTOM).build();
      } catch (CloneNotSupportedException e) {
      }
    }
    InAppMessageComponent inAppMessageComponent =
        DaggerInAppMessageComponent.builder()
            .inflaterModule(new InflaterModule(inAppMessage, updatedConfig, activity))
            .build();
    return inAppMessageComponent.bannerBindingWrapper();
  }

  public BindingWrapper createCardBindingWrapper(
          Activity activity, InAppMessageLayoutConfig config, InAppMessage inAppMessage) {
    InAppMessageComponent inAppMessageComponent =
        DaggerInAppMessageComponent.builder()
            .inflaterModule(new InflaterModule(inAppMessage, config, activity))
            .build();
    return inAppMessageComponent.cardBindingWrapper();
  }
}

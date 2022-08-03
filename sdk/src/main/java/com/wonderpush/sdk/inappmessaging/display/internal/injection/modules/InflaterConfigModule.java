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

package com.wonderpush.sdk.inappmessaging.display.internal.injection.modules;

import android.content.res.Configuration;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.wonderpush.sdk.inappmessaging.display.internal.InAppMessageLayoutConfig;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.keys.LayoutConfigKey;
import com.wonderpush.sdk.inappmessaging.model.MessageType;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;

/** @hide */
@Module
public class InflaterConfigModule {

  @SuppressWarnings("deprecation")
  private static final int FLAG_LAYOUT_INSET_DECOR = WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;

  // visible for testing
  public static int DISABLED_BG_FLAG =
      WindowManager.LayoutParams.FLAG_DIM_BEHIND
          | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
          | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
          | FLAG_LAYOUT_INSET_DECOR;

  public static int DISMISSIBLE_DIALOG_FLAG =
      WindowManager.LayoutParams.FLAG_DIM_BEHIND
          | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
          | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
          | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
          | FLAG_LAYOUT_INSET_DECOR;

  private int ENABLED_BG_FLAG =
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
          | FLAG_LAYOUT_INSET_DECOR
          | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

  public static String configFor(MessageType type, int orientation) {
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      switch (type) {
        case MODAL:
          return LayoutConfigKey.MODAL_PORTRAIT;
        case CARD:
          return LayoutConfigKey.CARD_PORTRAIT;
        case IMAGE_ONLY:
          return LayoutConfigKey.IMAGE_ONLY_PORTRAIT;
        case BANNER:
          return LayoutConfigKey.BANNER_PORTRAIT;
        case WEBVIEW:
          return LayoutConfigKey.WEBVIEW_PORTRAIT;
        default:
          return null;
      }
    } else {
      switch (type) {
        case MODAL:
          return LayoutConfigKey.MODAL_LANDSCAPE;
        case CARD:
          return LayoutConfigKey.CARD_LANDSCAPE;
        case IMAGE_ONLY:
          return LayoutConfigKey.IMAGE_ONLY_LANDSCAPE;
        case BANNER:
          return LayoutConfigKey.BANNER_LANDSCAPE;
        case WEBVIEW:
          return LayoutConfigKey.WEBVIEW_LANDSCAPE;
        default:
          return null;
      }
    }
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.IMAGE_ONLY_PORTRAIT)
  public InAppMessageLayoutConfig providesPortraitImageLayoutConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxDialogHeightRatio(0.9f)
        .setMaxDialogWidthRatio(0.9f)
        .setMaxImageWidthWeight(0.8f)
        .setMaxImageHeightWeight(0.8f)
        .setViewWindowGravity(Gravity.CENTER)
        .setWindowFlag(DISABLED_BG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        .setAutoDismiss(false)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.IMAGE_ONLY_LANDSCAPE)
  public InAppMessageLayoutConfig providesLandscapeImageLayoutConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxDialogHeightRatio(0.9f)
        .setMaxDialogWidthRatio(0.9f)
        .setMaxImageWidthWeight(0.8f)
        .setMaxImageHeightWeight(0.8f)
        .setViewWindowGravity(Gravity.CENTER)
        .setWindowFlag(DISABLED_BG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        .setAutoDismiss(false)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.MODAL_LANDSCAPE)
  public InAppMessageLayoutConfig providesModalLandscapeConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxDialogHeightRatio(0.8f)
        .setMaxDialogWidthRatio(1f)
        .setMaxImageHeightWeight(1f) // entire dialog height
        .setMaxImageWidthWeight(0.4f)
        .setMaxBodyHeightWeight(0.6f)
        .setMaxBodyWidthWeight(0.4f)
        .setViewWindowGravity(Gravity.CENTER)
        .setWindowFlag(DISABLED_BG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        .setAutoDismiss(false)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.MODAL_PORTRAIT)
  public InAppMessageLayoutConfig providesModalPortraitConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxDialogHeightRatio(0.8f)
        .setMaxDialogWidthRatio(0.7f)
        .setMaxImageHeightWeight(0.6f)
        .setMaxBodyHeightWeight(0.1f)
        .setMaxImageWidthWeight(0.9f) // entire dialog width
        .setMaxBodyWidthWeight(0.9f) // entire dialog width
        .setViewWindowGravity(Gravity.CENTER)
        .setWindowFlag(DISABLED_BG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        .setAutoDismiss(false)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.CARD_LANDSCAPE)
  public InAppMessageLayoutConfig providesCardLandscapeConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxDialogHeightRatio(0.8f)
        .setMaxDialogWidthRatio(1f)
        .setMaxImageHeightWeight(1f) // entire dialog height
        .setMaxImageWidthWeight(0.5f)
        .setViewWindowGravity(Gravity.CENTER)
        .setWindowFlag(DISMISSIBLE_DIALOG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        .setAutoDismiss(false)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.CARD_PORTRAIT)
  public InAppMessageLayoutConfig providesCardPortraitConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxDialogHeightRatio(0.8f)
        .setMaxDialogWidthRatio(0.7f)
        .setMaxImageHeightWeight(0.6f)
        .setMaxImageWidthWeight(1f) // entire dialog width
        .setMaxBodyHeightWeight(0.1f)
        .setMaxBodyWidthWeight(0.9f) // entire dialog width
        .setViewWindowGravity(Gravity.CENTER)
        .setWindowFlag(DISMISSIBLE_DIALOG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        .setAutoDismiss(false)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.BANNER_PORTRAIT)
  public InAppMessageLayoutConfig providesBannerPortraitLayoutConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxImageHeightWeight(0.3f)
        .setMaxImageWidthWeight(0.3f)
        .setMaxDialogHeightRatio(0.5f)
        .setMaxDialogWidthRatio(0.9f)
        .setViewWindowGravity(Gravity.TOP)
        .setWindowFlag(ENABLED_BG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        .setAutoDismiss(true)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.BANNER_LANDSCAPE)
  public InAppMessageLayoutConfig providesBannerLandscapeLayoutConfig() {
    return InAppMessageLayoutConfig.builder()
        .setMaxImageHeightWeight(0.3f)
        .setMaxImageWidthWeight(0.3f)
        .setMaxDialogHeightRatio(0.5f)
        .setMaxDialogWidthRatio(0.9f)
        .setViewWindowGravity(Gravity.TOP)
        .setWindowFlag(ENABLED_BG_FLAG)
        .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        .setWindowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        .setAutoDismiss(true)
        .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.WEBVIEW_PORTRAIT)
  public InAppMessageLayoutConfig providesPortraitWebViewLayoutConfig() {
    return InAppMessageLayoutConfig.builder()
            .setMaxDialogHeightRatio(1f)
            .setMaxDialogWidthRatio(1f)
            .setViewWindowGravity(Gravity.CENTER)
            .setWindowFlag(DISABLED_BG_FLAG)
            .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
            .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
            .setAutoDismiss(false)
            .build();
  }

  // visible for testing
  @Provides
  @IntoMap
  @StringKey(LayoutConfigKey.WEBVIEW_LANDSCAPE)
  public InAppMessageLayoutConfig providesLandscapeWebViewLayoutConfig() {
    return InAppMessageLayoutConfig.builder()
            .setMaxDialogHeightRatio(1f)
            .setMaxDialogWidthRatio(1f)
            .setViewWindowGravity(Gravity.CENTER)
            .setWindowFlag(DISABLED_BG_FLAG)
            .setWindowWidth(ViewGroup.LayoutParams.MATCH_PARENT)
            .setWindowHeight(ViewGroup.LayoutParams.MATCH_PARENT)
            .setAutoDismiss(false)
            .build();
  }
}

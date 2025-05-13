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

package com.wonderpush.sdk.inappmessaging.display.internal.bindingwrappers;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import android.graphics.Rect;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.wonderpush.sdk.R;
import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import com.wonderpush.sdk.inappmessaging.display.internal.InAppMessageLayoutConfig;
import com.wonderpush.sdk.inappmessaging.display.internal.ResizableImageView;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessageScope;
import com.wonderpush.sdk.inappmessaging.display.internal.layout.IamFrameLayout;
import com.wonderpush.sdk.inappmessaging.model.BannerMessage;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.inappmessaging.model.MessageType;

import java.util.List;

import javax.inject.Inject;

/** @hide */
@InAppMessageScope
@SuppressWarnings("Convert2Lambda")
public class BannerBindingWrapper extends BindingWrapper {

  private IamFrameLayout bannerRoot;
  private ViewGroup bannerContentRoot;

  private TextView bannerBody;
  private ResizableImageView bannerImage;
  private TextView bannerTitle;

  private View.OnClickListener mDismissListener;

  private boolean dismissedOnSwipe;

  @Inject
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public BannerBindingWrapper(
          Rect displayBounds, InAppMessageLayoutConfig config, LayoutInflater inflater, InAppMessage message) {
    super(displayBounds, config, inflater, message);
  }

  @Nullable
  @Override
  public ViewTreeObserver.OnGlobalLayoutListener inflate(
      List<View.OnClickListener> actionListeners,
      View.OnClickListener dismissOnClickListener) {

    View root = inflater.inflate(R.layout.wonderpush_android_sdk_banner, null);
    bannerRoot = root.findViewById(R.id.banner_root);
    bannerContentRoot = root.findViewById(R.id.banner_content_root);
    bannerBody = root.findViewById(R.id.banner_body);
    bannerImage = root.findViewById(R.id.banner_image);
    bannerTitle = root.findViewById(R.id.banner_title);

    if (message.getMessageType().equals(MessageType.BANNER)) {
      BannerMessage bannerMessage = (BannerMessage) message;
      setMessage(bannerMessage);
      setLayoutConfig(config);
      setSwipeDismissListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          dismissedOnSwipe = true;
          dismissOnClickListener.onClick(view);
        }
      });
      if (actionListeners.size() > 0) setActionListener(actionListeners.get(0));
    }
    return null;
  }

  private void setMessage(@NonNull BannerMessage message) {
    if (!TextUtils.isEmpty(message.getBackgroundHexColor())) {
      setViewBgColorFromHex(bannerContentRoot, message.getBackgroundHexColor());
    }

    bannerImage.setVisibility(
        (message.getImageUrl() == null)
            ? View.GONE
            : View.VISIBLE);

    if (message.getTitle() != null) {
      if (!TextUtils.isEmpty(message.getTitle().getText())) {
        bannerTitle.setText(message.getTitle().getText());
      }

      setViewTextColorFromHex(bannerTitle, message.getTitle().getHexColor());
    }

    if (message.getBody() != null) {
      if (!TextUtils.isEmpty(message.getBody().getText())) {
        bannerBody.setText(message.getBody().getText());
      }

      setViewTextColorFromHex(bannerBody, message.getBody().getHexColor());
    }
  }

  private void setLayoutConfig(InAppMessageLayoutConfig layoutConfig) {
    // TODO: Document why the width is the min of the max width and height
    int bannerWidth = (int) Math.min(layoutConfig.maxDialogWidthRatio() * this.displayBounds.width(), layoutConfig.maxDialogHeightRatio() * this.displayBounds.height());

    ViewGroup.LayoutParams params = bannerRoot.getLayoutParams();
    if (params == null) {
      params =
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    params.width = bannerWidth;

    bannerRoot.setLayoutParams(params);

    bannerImage.setMaxHeight((int) (layoutConfig.getMaxImageHeightRatio() * this.displayBounds.height()));
    bannerImage.setMaxWidth((int) (layoutConfig.getMaxImageWidthRatio() * this.displayBounds.width()));
  }

  private void setSwipeDismissListener(final View.OnClickListener dismissListener) {
    mDismissListener = dismissListener;
    bannerRoot.setDismissListener(mDismissListener);
  }

  private void setActionListener(View.OnClickListener actionListener) {
    bannerContentRoot.setOnClickListener(actionListener);
  }

  @NonNull
  @Override
  public InAppMessageLayoutConfig getConfig() {
    return config;
  }

  @Nullable
  @Override
  public View getDismissView() {
    return null;
  }

  @Nullable
  @Override
  public ImageView getImageView() {
    return bannerImage;
  }

  @Nullable
  @Override
  public WebView getWebView() {
    return null;
  }

  @NonNull
  @Override
  public ViewGroup getRootView() {
    return bannerRoot;
  }

  @Nullable
  @Override
  public View getDialogView() {
    return bannerContentRoot;
  }

  @Nullable
  @Override
  public View.OnClickListener getDismissListener() {
    return mDismissListener;
  }

  @Override
  public boolean canSwipeToDismiss() {
    return true;
  }

  @Nullable
  @Override
  public IamAnimator.EntryAnimation getEntryAnimation() {
    if (message instanceof BannerMessage) {
      BannerMessage bannerMessage = (BannerMessage)message;
      switch (bannerMessage.getBannerPosition()) {
        case BOTTOM: return IamAnimator.EntryAnimation.SLIDE_IN_FROM_BOTTOM;
        case TOP: return IamAnimator.EntryAnimation.SLIDE_IN_FROM_TOP;
      }
    }
    return super.getEntryAnimation();
  }

  @NonNull

  @Nullable
  @Override
  public IamAnimator.ExitAnimation getExitAnimation() {
    // No animation when we swiped
    if (dismissedOnSwipe) return null;
    if (message instanceof BannerMessage) {
      BannerMessage bannerMessage = (BannerMessage)message;
      switch (bannerMessage.getBannerPosition()) {
        case BOTTOM: return IamAnimator.ExitAnimation.SLIDE_OUT_DOWN;
        case TOP: return IamAnimator.ExitAnimation.SLIDE_OUT_TOP;
      }
    }
    return null;
  }
}

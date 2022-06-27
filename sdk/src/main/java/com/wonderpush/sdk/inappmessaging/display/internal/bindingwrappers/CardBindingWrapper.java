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
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import android.graphics.Rect;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.R;
import com.wonderpush.sdk.inappmessaging.display.internal.InAppMessageLayoutConfig;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessageScope;
import com.wonderpush.sdk.inappmessaging.display.internal.layout.BaseModalLayout;
import com.wonderpush.sdk.inappmessaging.display.internal.layout.IamFrameLayout;
import com.wonderpush.sdk.inappmessaging.model.CardMessage;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.inappmessaging.model.MessageType;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

/** @hide */
@InAppMessageScope
public class CardBindingWrapper extends BindingWrapper {

  private IamFrameLayout cardRoot;
  private BaseModalLayout cardContentRoot;
  private ScrollView bodyScroll;
  private Button primaryButton;
  private Button secondaryButton;
  private ImageView imageView;
  private TextView messageBody;
  private TextView messageTitle;
  private CardMessage cardMessage;
  private View.OnClickListener dismissListener;

  private ViewTreeObserver.OnGlobalLayoutListener layoutListener =
      new ScrollViewAdjustableListener();

  @Inject
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public CardBindingWrapper(
      Rect displayBounds, InAppMessageLayoutConfig config, LayoutInflater inflater, InAppMessage message) {
    super(displayBounds, config, inflater, message);
  }

  @NonNull
  @Override
  public ViewTreeObserver.OnGlobalLayoutListener inflate(
      List<View.OnClickListener> actionListeners,
      View.OnClickListener dismissOnClickListener) {

    View root = inflater.inflate(R.layout.wonderpush_android_sdk_card, null);
    bodyScroll = root.findViewById(R.id.body_scroll);
    primaryButton = root.findViewById(R.id.primary_button);
    secondaryButton = root.findViewById(R.id.secondary_button);
    imageView = root.findViewById(R.id.image_view);
    messageBody = root.findViewById(R.id.message_body);
    messageTitle = root.findViewById(R.id.message_title);
    cardRoot = root.findViewById(R.id.card_root);
    cardContentRoot = root.findViewById(R.id.card_content_root);

    if (message.getMessageType().equals(MessageType.CARD)) {
      cardMessage = (CardMessage) message;
      setMessage(cardMessage);
      setImage(cardMessage);
      setButtons(actionListeners);
      setLayoutConfig(config);
      setDismissListener(dismissOnClickListener);
      setViewBgColorFromHex(cardContentRoot, cardMessage.getBackgroundHexColor());
    }
    return layoutListener;
  }

  @Nullable
  @Override
  public ImageView getImageView() {
    return imageView;
  }

  @Nullable
  @Override
  public WebView getWebView() {
    return null;
  }

  @NonNull
  public View getScrollView() {
    return bodyScroll;
  }

  @Nullable
  public View getTitleView() {
    return messageTitle;
  }

  @Nullable
  @Override
  public View getDismissView() {
    return cardRoot;
  }

  @Nullable
  @Override
  public ViewGroup getRootView() {
    return cardRoot;
  }

  @Nullable
  @Override
  public View getDialogView() {
    return cardContentRoot;
  }

  @NonNull
  @Override
  public InAppMessageLayoutConfig getConfig() {
    return config;
  }

  @Nullable
  @Override
  public View.OnClickListener getDismissListener() {
    return dismissListener;
  }

  @Nullable
  public Button getPrimaryButton() {
    return primaryButton;
  }

  @Nullable
  public Button getSecondaryButton() {
    return secondaryButton;
  }

  private void setMessage(CardMessage message) {
    // We can assume we have a title because the CardMessage model enforces it.
    messageTitle.setText(message.getTitle().getText());
    if (!TextUtils.isEmpty(message.getTitle().getHexColor())) messageTitle.setTextColor(Color.parseColor(message.getTitle().getHexColor()));

    // Right now we need to check for null, eventually we will make an API change to have hasBody()
    // Additionally right now we have to check for getText. this will be fixed soon.
    if (message.getBody() != null && message.getBody().getText() != null) {
      bodyScroll.setVisibility(View.VISIBLE);
      messageBody.setVisibility(View.VISIBLE);
      messageBody.setText(message.getBody().getText());
      if (!TextUtils.isEmpty(message.getBody().getHexColor())) messageBody.setTextColor(Color.parseColor(message.getBody().getHexColor()));
    } else {
      bodyScroll.setVisibility(View.GONE);
      messageBody.setVisibility(View.GONE);
    }
  }

  private void setButtons(List<View.OnClickListener> actionListeners) {
    List<ActionModel> primaryActions = cardMessage.getPrimaryActions();
    List<ActionModel> secondaryActions = cardMessage.getSecondaryActions();
    com.wonderpush.sdk.inappmessaging.model.Button primaryActionButton = cardMessage.getPrimaryButton();
    com.wonderpush.sdk.inappmessaging.model.Button secondaryActionButton = cardMessage.getSecondaryButton();

    // Primary button will always exist.
    setupViewButtonFromModel(primaryButton, primaryActionButton);
    // The main display code will override the action listener with a dismiss listener in the case
    // of a missing action url.
    if (actionListeners.size() > 0)setButtonActionListener(primaryButton, actionListeners.get(0));
    primaryButton.setVisibility(View.VISIBLE);

    // Secondary button is optional, eventually this null check will be at the model level.
    if (secondaryActionButton != null) {
      setupViewButtonFromModel(secondaryButton, secondaryActionButton);
      if (actionListeners.size() > 1) setButtonActionListener(secondaryButton, actionListeners.get(1));
      secondaryButton.setVisibility(View.VISIBLE);
    } else {
      secondaryButton.setVisibility(View.GONE);
    }
  }

  private void setImage(CardMessage message) {
    // Right now we need to check for null, eventually we will make an API change hasImageUrl()
    if (message.getPortraitImageUrl() != null || message.getLandscapeImageUrl() != null) {
      imageView.setVisibility(View.VISIBLE);
    } else {
      imageView.setVisibility(View.GONE);
    }
  }

  private void setLayoutConfig(InAppMessageLayoutConfig config) {
    imageView.setMaxHeight((int) (config.getMaxImageHeightRatio() * this.displayBounds.height()));
    imageView.setMaxWidth((int) (config.getMaxImageWidthRatio() * this.displayBounds.width()));
  }

  private void setDismissListener(View.OnClickListener dismissListener) {
    this.dismissListener = dismissListener;
    cardRoot.setDismissListener(dismissListener);
  }

  @VisibleForTesting
  public void setLayoutListener(ViewTreeObserver.OnGlobalLayoutListener listener) {
    layoutListener = listener;
  }

  // TODO: Kill this.
  public class ScrollViewAdjustableListener implements ViewTreeObserver.OnGlobalLayoutListener {
    @Override
    public void onGlobalLayout() {
      imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }
  }
}

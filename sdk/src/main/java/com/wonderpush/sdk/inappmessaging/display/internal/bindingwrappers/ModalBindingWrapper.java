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
import com.wonderpush.sdk.inappmessaging.display.internal.layout.IamRelativeLayout;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.inappmessaging.model.MessageType;
import com.wonderpush.sdk.inappmessaging.model.ModalMessage;

import java.util.List;

import javax.inject.Inject;

/** @hide */
@InAppMessageScope
public class ModalBindingWrapper extends BindingWrapper {

  private IamRelativeLayout modalRoot;
  private ViewGroup modalContentRoot;

  private ScrollView bodyScroll;
  private Button button;
  private View collapseImage;
  private ImageView imageView;
  private TextView messageBody;
  private TextView messageTitle;
  private ModalMessage modalMessage;

  private ViewTreeObserver.OnGlobalLayoutListener layoutListener =
      new ScrollViewAdjustableListener();

  @Inject
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public ModalBindingWrapper(
          Rect displayBounds, InAppMessageLayoutConfig config, LayoutInflater inflater, InAppMessage message) {
    super(displayBounds, config, inflater, message);
  }

  @NonNull
  @Override
  public ViewTreeObserver.OnGlobalLayoutListener inflate(
      List<View.OnClickListener> actionListeners,
      View.OnClickListener dismissOnClickListener) {

    View root = inflater.inflate(R.layout.wonderpush_android_sdk_modal, null);
    bodyScroll = root.findViewById(R.id.body_scroll);
    button = root.findViewById(R.id.button);
    collapseImage = root.findViewById(R.id.collapse_button);
    imageView = root.findViewById(R.id.image_view);
    messageBody = root.findViewById(R.id.message_body);
    messageTitle = root.findViewById(R.id.message_title);
    modalRoot = root.findViewById(R.id.modal_root);

    modalContentRoot = root.findViewById(R.id.modal_content_root);

    if (message.getMessageType().equals(MessageType.MODAL)) {
      modalMessage = (ModalMessage) message;
      setMessage(modalMessage);
      setButton(actionListeners);
      setLayoutConfig(config);
      setDismissListener(dismissOnClickListener);
      setViewBgColorFromHex(modalContentRoot, modalMessage.getBackgroundHexColor());

      if (collapseImage.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)collapseImage.getLayoutParams();
        float density = inflater.getContext().getResources().getDisplayMetrics().density;
        switch (modalMessage.getCloseButtonPosition()) {
          case NONE:
            collapseImage.setVisibility(View.GONE);
            break;
          case INSIDE:
            collapseImage.setVisibility(View.VISIBLE);
            layoutParams.topMargin = (int)(density * 5);
            layoutParams.rightMargin = (int)(density * 5);
            break;
          case OUTSIDE:
            collapseImage.setVisibility(View.VISIBLE);
            layoutParams.topMargin = (int)(density * -12);
            layoutParams.rightMargin = (int)(density * -12);
            break;
        }
        collapseImage.setLayoutParams(layoutParams);
      }

    }
    return layoutListener;
  }

  @Nullable
  @Override
  public View getDismissView() {
    return modalRoot;
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
  @Override
  public ViewGroup getRootView() {
    return modalRoot;
  }

  @Nullable
  @Override
  public View getDialogView() {
    return modalContentRoot;
  }

  @Nullable
  public View getScrollView() {
    return bodyScroll;
  }

  @Nullable
  public View getTitleView() {
    return messageTitle;
  }

  @NonNull
  @Override
  public InAppMessageLayoutConfig getConfig() {
    return config;
  }

  @Nullable
  public Button getActionButton() {
    return button;
  }

  @Nullable
  public View getCollapseButton() {
    return collapseImage;
  }

  private void setMessage(ModalMessage message) {
    if (message.getImageUrl() == null || TextUtils.isEmpty(message.getImageUrl())) {
      imageView.setVisibility(View.GONE);
    } else {
      imageView.setVisibility(View.VISIBLE);
    }

    if (message.getTitle() != null) {
      if (!TextUtils.isEmpty(message.getTitle().getText())) {
        messageTitle.setVisibility(View.VISIBLE);
        messageTitle.setText(message.getTitle().getText());
      } else {
        messageTitle.setVisibility(View.GONE);
      }

      setViewTextColorFromHex(messageTitle, message.getTitle().getHexColor());
    }

    // eventually we should no longer need to check for the text of the body
    if (message.getBody() != null && !TextUtils.isEmpty(message.getBody().getText())) {
      bodyScroll.setVisibility(View.VISIBLE);
      messageBody.setVisibility(View.VISIBLE);
      setViewTextColorFromHex(messageBody, message.getBody().getHexColor());
      messageBody.setText(message.getBody().getText());
    } else {
      bodyScroll.setVisibility(View.GONE);
      messageBody.setVisibility(View.GONE);
    }
  }

  private void setButton(List<View.OnClickListener> actionListeners) {
    List<ActionModel> modalActions = modalMessage.getActions();
    com.wonderpush.sdk.inappmessaging.model.Button modalButton = modalMessage.getButton();
    // Right now we have to check for text not being empty but this should be fixed in the future
    if (modalButton != null
        && !TextUtils.isEmpty(modalButton.getText().getText())) {
      setupViewButtonFromModel(button, modalButton);
      if (actionListeners.size() > 0) setButtonActionListener(button, actionListeners.get(0));
      button.setVisibility(View.VISIBLE);
    } else {
      button.setVisibility(View.GONE);
    }
  }

  private void setLayoutConfig(InAppMessageLayoutConfig config) {
    imageView.setMaxHeight((int) (config.getMaxImageHeightRatio() * this.displayBounds.height()));
    imageView.setMaxWidth((int) (config.getMaxImageWidthRatio() * this.displayBounds.width()));
  }

  private void setDismissListener(View.OnClickListener dismissListener) {
    collapseImage.setOnClickListener(dismissListener);
    modalRoot.setDismissListener(dismissListener);
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

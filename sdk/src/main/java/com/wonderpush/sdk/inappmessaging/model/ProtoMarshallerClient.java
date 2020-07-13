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

package com.wonderpush.sdk.inappmessaging.model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.NotificationMetadata;
import com.wonderpush.sdk.inappmessaging.internal.Logging;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Class to transform internal proto representation to externalized parcelable objects. See {@link
 * InAppMessage}.
 *
 * <p>Note that an object is inflated only if it is defined in the proto and is null otherwise.
 *
 * @hide
 */
@Singleton
public class ProtoMarshallerClient {

  @Inject
  ProtoMarshallerClient() {}

  @Nonnull
  private static ModalMessage.Builder from(MessagesProto.ModalMessage in) {
    ModalMessage.Builder builder = ModalMessage.builder();

    if (!TextUtils.isEmpty(in.getBackgroundHexColor())) {
      builder.setBackgroundHexColor(in.getBackgroundHexColor());
    }

    if (!TextUtils.isEmpty(in.getImageUrl())) {
      builder.setImageData(ImageData.builder().setImageUrl(in.getImageUrl()).build());
    }

    if (in.hasActions()) {
      builder.setActions(in.getActions());
      builder.setButton(decode(in.getActionButton()));
    }

    if (in.hasBody()) {
      builder.setBody(decode(in.getBody()));
    }

    if (in.hasTitle()) {
      builder.setTitle(decode(in.getTitle()));
    }

    return builder;
  }

  @Nonnull
  private static ImageOnlyMessage.Builder from(MessagesProto.ImageOnlyMessage in) {
    ImageOnlyMessage.Builder builder = ImageOnlyMessage.builder();

    if (!TextUtils.isEmpty(in.getImageUrl())) {
      builder.setImageData(ImageData.builder().setImageUrl(in.getImageUrl()).build());
    }

    if (in.hasActions()) {
      builder.setActions(in.getActions());
    }

    return builder;
  }

  @Nonnull
  private static BannerMessage.Builder from(MessagesProto.BannerMessage in) {
    BannerMessage.Builder builder = BannerMessage.builder();

    if (!TextUtils.isEmpty(in.getBackgroundHexColor())) {
      builder.setBackgroundHexColor(in.getBackgroundHexColor());
    }

    if (!TextUtils.isEmpty(in.getImageUrl())) {
      builder.setImageData(ImageData.builder().setImageUrl(in.getImageUrl()).build());
    }

    if (in.hasActions()) {
      builder.setActions(in.getActions());
    }

    if (in.hasBody()) {
      builder.setBody(decode(in.getBody()));
    }

    if (in.hasTitle()) {
      builder.setTitle(decode(in.getTitle()));
    }

    return builder;
  }

  @Nonnull
  private static CardMessage.Builder from(MessagesProto.CardMessage in) {
    CardMessage.Builder builder = CardMessage.builder();

    if (in.hasTitle()) {
      builder.setTitle(decode(in.getTitle()));
    }

    if (in.hasBody()) {
      builder.setBody(decode(in.getBody()));
    }

    if (!TextUtils.isEmpty(in.getBackgroundHexColor())) {
      builder.setBackgroundHexColor(in.getBackgroundHexColor());
    }

    if (in.hasPrimaryActions() || in.hasPrimaryActionButton()) {
      builder.setPrimaryActions(in.getPrimaryActions());
      builder.setPrimaryButton(decode(in.getPrimaryActionButton()));
    }

    if (in.hasSecondaryActions() || in.hasSecondaryActionButton()) {
      builder.setSecondaryActions(in.getSecondaryActions());
      builder.setSecondaryButton(decode(in.getSecondaryActionButton()));
    }

    if (!TextUtils.isEmpty(in.getPortraitImageUrl())) {
      builder.setPortraitImageData(
          ImageData.builder().setImageUrl(in.getPortraitImageUrl()).build());
    }

    if (!TextUtils.isEmpty(in.getLandscapeImageUrl())) {
      builder.setLandscapeImageData(
          ImageData.builder().setImageUrl(in.getLandscapeImageUrl()).build());
    }

    return builder;
  }

  private static Button decode(MessagesProto.Button in) {
    Button.Builder builder = Button.builder();

    if (!TextUtils.isEmpty(in.getButtonHexColor())) {
      builder.setButtonHexColor(in.getButtonHexColor());
    }

    if (in.hasText()) {
      builder.setText(decode(in.getText()));
    }
    return builder.build();
  }

  private static Text decode(MessagesProto.Text in) {
    Text.Builder builder = Text.builder();

    if (!TextUtils.isEmpty(in.getHexColor())) {
      builder.setHexColor(in.getHexColor());
    }

    if (!TextUtils.isEmpty(in.getText())) {
      builder.setText(in.getText());
    }

    return builder.build();
  }

  /** Tranform {@link MessagesProto.Content} proto to an {@link InAppMessage} value object */
  public static InAppMessage decode(
      @NonNull MessagesProto.Content in,
      @NonNull String campaignId,
      @NonNull String notificationId,
      String viewId,
      boolean isTestMessage) {
    Logging.logd("Decoding message: " + in.toString());
    NotificationMetadata notificationMetadata =
        new NotificationMetadata(campaignId, notificationId, viewId, isTestMessage);

    switch (in.getMessageDetailsCase()) {
      case BANNER:
        return from(in.getBanner()).build(notificationMetadata, in.getDataBundle());
      case IMAGE_ONLY:
        return from(in.getImageOnly()).build(notificationMetadata, in.getDataBundle());
      case MODAL:
        return from(in.getModal()).build(notificationMetadata, in.getDataBundle());
      case CARD:
        return from(in.getCard()).build(notificationMetadata, in.getDataBundle());

      default:
        // If the template is unsupported, then we return an unsupported message
        return new InAppMessage(
            new NotificationMetadata(campaignId, notificationId, viewId, isTestMessage),
            MessageType.UNSUPPORTED,
            in.getDataBundle()) {

          @Override
          public ButtonType getButtonType(List<ActionModel> actions) {
            return ButtonType.UNDEFINED;
          }
        };
    }
  }
}

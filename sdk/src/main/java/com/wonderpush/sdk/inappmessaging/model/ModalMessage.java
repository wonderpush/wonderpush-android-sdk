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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.JSONUtil;
import com.wonderpush.sdk.NotificationMetadata;

import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import org.json.JSONObject;

import java.util.List;

/** Encapsulates an In App Modal Message. */
public class ModalMessage extends InAppMessage implements InAppMessage.InAppMessageWithImage {
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  @NonNull private final Text title;

  @Nullable private final Text body;
  @Nullable private final String imageUrl;
  @NonNull private final List<ActionModel> actions;
  @Nullable private final Button button;
  @NonNull private final String backgroundHexColor;
  @NonNull private final CloseButtonPosition closeButtonPosition;

  public static ModalMessage create(NotificationMetadata notificationMetadata, JSONObject payloadJson, JSONObject modalJson) throws Campaign.InvalidJsonException {

    // Title
    Text titleText = Text.fromJSON(modalJson.optJSONObject("title"));
    if (titleText == null) throw new Campaign.InvalidJsonException("Missing title text");

    // Body
    Text bodyText = Text.fromJSON(modalJson.optJSONObject("body"));

    // Image
    String imageUrl = JSONUtil.optString(modalJson, "imageUrl");

    // Background color
    String backgroundHexColor = JSONUtil.optString(modalJson, "backgroundHexColor", "#FFFFFF");

    // Action & button
    Button actionButton = Button.fromJSON(modalJson.optJSONObject("actionButton"));

    List<ActionModel> actions = ActionModel.from(modalJson.optJSONArray("actions"));

    String closeButtonPositionString = JSONUtil.optString(modalJson, "closeButtonPosition", "outside");
    InAppMessage.CloseButtonPosition closeButtonPosition = InAppMessage.CloseButtonPosition.OUTSIDE;
    if ("inside".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.INSIDE;
    if ("none".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.NONE;

    // Animations
    IamAnimator.EntryAnimation entryAnimation = IamAnimator.EntryAnimation.fromSlug(modalJson.optString("entryAnimation", "fadeIn"));
    IamAnimator.ExitAnimation exitAnimation = IamAnimator.ExitAnimation.fromSlug(modalJson.optString("exitAnimation", "fadeOut"));
    return new ModalMessage(notificationMetadata, titleText, bodyText, imageUrl, actions, actionButton, backgroundHexColor, closeButtonPosition, entryAnimation, exitAnimation, payloadJson);
  }

  /** @hide */
  @Override
  public int hashCode() {
    int bodyHash = body != null ? body.hashCode() : 0;
    int actionHash = 0;
    for(ActionModel action : actions) actionHash += action.hashCode();
    int imageHash = imageUrl != null ? imageUrl.hashCode() : 0;
    int buttonHash = button != null ? button.hashCode() : 0;
    return title.hashCode() + bodyHash + backgroundHexColor.hashCode() + actionHash + imageHash + buttonHash + closeButtonPosition.hashCode() + entryAnimation.hashCode() + exitAnimation.hashCode();
  }

  /** @hide */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true; // same instance
    }
    if (!(o instanceof ModalMessage)) {
      return false; // not the correct instance type
    }
    ModalMessage m = (ModalMessage) o;
    if (hashCode() != m.hashCode()) {
      return false; // the hashcodes don't match
    }
    if (closeButtonPosition != m.closeButtonPosition) return false;
    if (entryAnimation != m.entryAnimation) return false;
    if (exitAnimation != m.exitAnimation) return false;
    if ((body == null && m.body != null) || (body != null && !body.equals(m.body))) {
      return false; // the bodies don't match
    }
    if ((actions == null && m.actions != null) || (actions != null && !actions.equals(m.actions))) {
      return false; // the actions don't match
    }
    if ((imageUrl == null && m.imageUrl != null)
        || (imageUrl != null && !imageUrl.equals(m.imageUrl))) {
      return false; // the image data don't match
    }
    if (!title.equals(m.title)) {
      return false; // the titles don't match
    }
    if ((button == null && m.button != null) || (button != null && !button.equals(m.button))) {
      return false;
    }
    if (backgroundHexColor.equals(m.backgroundHexColor)) {
      return true; // everything matches
    }
    return false;
  }
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  private ModalMessage(
      @NonNull NotificationMetadata notificationMetadata,
      @NonNull Text title,
      @Nullable Text body,
      @Nullable String imageUrl,
      @NonNull List<ActionModel> actions,
      @Nullable Button button,
      @NonNull String backgroundHexColor,
      @NonNull CloseButtonPosition closeButtonPosition,
      @NonNull IamAnimator.EntryAnimation entryAnimation,
      @NonNull IamAnimator.ExitAnimation exitAnimation,
      @NonNull JSONObject data) {
    super(notificationMetadata, MessageType.MODAL, data, entryAnimation, exitAnimation);
    this.title = title;
    this.body = body;
    this.imageUrl = imageUrl;
    this.actions = actions;
    this.backgroundHexColor = backgroundHexColor;
    this.button = button;
    this.closeButtonPosition = closeButtonPosition;
  }

  /** Gets the title {@link Text} associated with this message */
  @NonNull
  public Text getTitle() {
    return title;
  }

  /** Gets the body {@link Text} associated with this message */
  @Nullable
  public Text getBody() {
    return body;
  }

  /** Gets the image associated with this message */
  @Nullable
  public String getImageUrl() {
    return imageUrl;
  }

  /** Gets the background hex color associated with this message */
  @NonNull
  public String getBackgroundHexColor() {
    return backgroundHexColor;
  }

  /** Gets the {@link ActionModel}s associated with this message */
  @NonNull
  public List<ActionModel> getActions() {
    return actions;
  }

  @Nullable
  public Button getButton() {
    return button;
  }

  @NonNull
  public CloseButtonPosition getCloseButtonPosition() {
    return closeButtonPosition;
  }

  @Override
  public ButtonType getButtonType(List<ActionModel> actions) {
    return actionsEqual(actions, this.actions) ? ButtonType.PRIMARY : ButtonType.UNDEFINED;
  }
}

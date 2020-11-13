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
import com.wonderpush.sdk.NotificationMetadata;

import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import org.json.JSONObject;

import java.util.List;

/** Encapsulates an In App Card Message. */
public class CardMessage extends InAppMessage {
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  @NonNull private final Text title;

  @Nullable private final Text body;
  @NonNull private final String backgroundHexColor;
  @NonNull private final List<ActionModel> primaryActions;
  @NonNull private final List<ActionModel> secondaryActions;
  @Nullable private final String portraitImageUrl;
  @Nullable private final String landscapeImageUrl;
  @Nullable private final Button primaryButton;
  @Nullable private final Button secondaryButton;

  public static CardMessage create(
          @NonNull NotificationMetadata notificationMetadata,
          JSONObject data,
          JSONObject content
  ) throws Campaign.InvalidJsonException {


    // Title
    JSONObject titleJson = content.optJSONObject("title");
    Text titleText = Text.fromJSON(titleJson);
    if (titleText == null) throw new Campaign.InvalidJsonException("Missing title text");
    // Body & Buttons
    Text bodyText = Text.fromJSON(content.optJSONObject("body"));

    // Images
    String portraitImageUrl =content.optString("portraitImageUrl", null);
    String landscapeImageUrl = content.optString("landscapeImageUrl", null);

    // Background (mandatory)
    String backgroundHexColor = content.optString("backgroundHexColor", "#FFFFFF");

    // Actions & buttons
    Button primaryActionButton = Button.fromJSON(content.optJSONObject("primaryActionButton"));
    Button secondaryActionButton = Button.fromJSON(content.optJSONObject("secondaryActionButton"));

    List<ActionModel> primaryActions = ActionModel.from(content.optJSONArray("primaryActions"));
    List<ActionModel> secondaryActions = ActionModel.from(content.optJSONArray("secondaryActions"));

    // Animations
    IamAnimator.EntryAnimation entryAnimation = IamAnimator.EntryAnimation.fromSlug(content.optString("entryAnimation", "fadeIn"));
    IamAnimator.ExitAnimation exitAnimation = IamAnimator.ExitAnimation.fromSlug(content.optString("exitAnimation", "fadeOut"));

    return new CardMessage(notificationMetadata, titleText, bodyText, portraitImageUrl, landscapeImageUrl, backgroundHexColor, primaryActions, secondaryActions, primaryActionButton, secondaryActionButton, entryAnimation, exitAnimation, data);
  }

  /** @hide */
  @Override
  public int hashCode() {
    int bodyHash = body != null ? body.hashCode() : 0;
    int secondaryActionHash = secondaryActions != null ? secondaryActions.hashCode() : 0;
    int portraitImageHash = portraitImageUrl != null ? portraitImageUrl.hashCode() : 0;
    int landscapeImageHash = landscapeImageUrl != null ? landscapeImageUrl.hashCode() : 0;
    int primaryButtonHash = primaryButton != null ? primaryButton.hashCode() : 0;
    int secondaryButtonHash = secondaryButton != null ? secondaryButton.hashCode() : 0;
    return title.hashCode()
        + bodyHash
        + backgroundHexColor.hashCode()
        + primaryActions.hashCode()
        + secondaryActionHash
        + portraitImageHash
        + landscapeImageHash
        + primaryButtonHash
        + secondaryButtonHash
        + entryAnimation.hashCode() + exitAnimation.hashCode();
  }

  /** @hide */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true; // same instance
    }
    if (!(o instanceof CardMessage)) {
      return false; // not the correct instance type
    }
    CardMessage c = (CardMessage) o;
    if (hashCode() != c.hashCode()) {
      return false; // the hashcodes don't match
    }
    if (entryAnimation != c.entryAnimation) return false;
    if (exitAnimation != c.exitAnimation) return false;
    if ((body == null && c.body != null) || (body != null && !body.equals(c.body))) {
      return false; // the bodies don't match
    }
    if ((secondaryActions == null && c.secondaryActions != null)
        || (secondaryActions != null && !secondaryActions.equals(c.secondaryActions))) {
      return false; // the secondary actions don't match
    }
    if ((secondaryButton == null && c.secondaryButton != null)
            || (secondaryButton != null && !secondaryButton.equals(c.secondaryButton))) {
      return false; // the secondary buttons don't match
    }
    if ((portraitImageUrl == null && c.portraitImageUrl != null)
        || (portraitImageUrl != null && !portraitImageUrl.equals(c.portraitImageUrl))) {
      return false; // the portrait image data don't match
    }
    if ((landscapeImageUrl == null && c.landscapeImageUrl != null)
        || (landscapeImageUrl != null && !landscapeImageUrl.equals(c.landscapeImageUrl))) {
      return false; // the landscape image data don't match
    }
    if (!title.equals(c.title)) {
      return false; // the titles don't match
    }
    if (!primaryActions.equals(c.primaryActions)) {
      return false; // the primary actions don't match
    }
    if ((primaryButton == null && c.primaryButton != null)
            || (primaryButton != null && !primaryButton.equals(c.primaryButton))) {
      return false; // the primary buttons don't match
    }
    if (backgroundHexColor.equals(c.backgroundHexColor)) {
      return true; // everything matches
    }
    return false;
  }
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  private CardMessage(
      @NonNull NotificationMetadata notificationMetadata,
      @NonNull Text title,
      @Nullable Text body,
      @Nullable String portraitImageUrl,
      @Nullable String landscapeImageUrl,
      @NonNull String backgroundHexColor,
      @NonNull List<ActionModel> primaryActions,
      @NonNull List<ActionModel> secondaryActions,
      @Nullable Button primaryButton,
      @Nullable Button secondaryButton,
      @NonNull IamAnimator.EntryAnimation entryAnimation,
      @NonNull IamAnimator.ExitAnimation exitAnimation,
      @NonNull JSONObject data) {
    super(notificationMetadata, MessageType.CARD, data, entryAnimation, exitAnimation);
    this.title = title;
    this.body = body;
    this.portraitImageUrl = portraitImageUrl;
    this.landscapeImageUrl = landscapeImageUrl;
    this.backgroundHexColor = backgroundHexColor;
    this.primaryActions = primaryActions;
    this.secondaryActions = secondaryActions;
    this.primaryButton = primaryButton;
    this.secondaryButton = secondaryButton;
  }

  /** Gets the image displayed when the phone is in a portrait orientation */
  @Nullable
  public String getPortraitImageUrl() {
    return portraitImageUrl;
  }

  /** Gets the image displayed when the phone is in a landcscape orientation */
  @Nullable
  public String getLandscapeImageUrl() {
    return landscapeImageUrl;
  }

  /** Gets the background hex color associated with this message */
  @NonNull
  public String getBackgroundHexColor() {
    return backgroundHexColor;
  }

  /**
   * Gets the primary {@link ActionModel}s associated with this message. If none is defined, the primary
   * action is 'dismiss'
   */
  @NonNull
  public List<ActionModel> getPrimaryActions() {
    return primaryActions;
  }

  /** Gets the secondary {@link ActionModel}s associated with this message */
  @NonNull
  public List<ActionModel> getSecondaryActions() {
    return secondaryActions;
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

  @Nullable
  public Button getPrimaryButton() {
    return primaryButton;
  }

  @Nullable
  public Button getSecondaryButton() {
    return secondaryButton;
  }

  @Override
  public ButtonType getButtonType(List<ActionModel> actions) {
    if (actionsEqual(actions, this.primaryActions)) return ButtonType.PRIMARY;
    if (actionsEqual(actions, this.secondaryActions)) return ButtonType.SECONDARY;
    return ButtonType.UNDEFINED;
  }
}

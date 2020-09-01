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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.NotificationMetadata;

import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import org.json.JSONObject;

import java.util.Collections;
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
  @Nullable private final ImageData portraitImageData;
  @Nullable private final ImageData landscapeImageData;
  @Nullable private final Button primaryButton;
  @Nullable private final Button secondaryButton;

  /** @hide */
  @Override
  public int hashCode() {
    int bodyHash = body != null ? body.hashCode() : 0;
    int secondaryActionHash = secondaryActions != null ? secondaryActions.hashCode() : 0;
    int portraitImageHash = portraitImageData != null ? portraitImageData.hashCode() : 0;
    int landscapeImageHash = landscapeImageData != null ? landscapeImageData.hashCode() : 0;
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
    if ((portraitImageData == null && c.portraitImageData != null)
        || (portraitImageData != null && !portraitImageData.equals(c.portraitImageData))) {
      return false; // the portrait image data don't match
    }
    if ((landscapeImageData == null && c.landscapeImageData != null)
        || (landscapeImageData != null && !landscapeImageData.equals(c.landscapeImageData))) {
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
      @Nullable ImageData portraitImageData,
      @Nullable ImageData landscapeImageData,
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
    this.portraitImageData = portraitImageData;
    this.landscapeImageData = landscapeImageData;
    this.backgroundHexColor = backgroundHexColor;
    this.primaryActions = primaryActions;
    this.secondaryActions = secondaryActions;
    this.primaryButton = primaryButton;
    this.secondaryButton = secondaryButton;
  }

  /** Gets the {@link ImageData} displayed when the phone is in a portrait orientation */
  @Nullable
  public ImageData getPortraitImageData() {
    return portraitImageData;
  }

  /** Gets the {@link ImageData} displayed when the phone is in a landcscape orientation */
  @Nullable
  public ImageData getLandscapeImageData() {
    return landscapeImageData;
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

  /**
   * only used by headless sdk and tests
   *
   * @hide
   */
  public static Builder builder() {
    return new CardMessage.Builder();
  }

  /**
   * Builder for {@link CardMessage}
   *
   * @hide
   */
  public static class Builder {
    @Nullable ImageData portraitImageData;
    @Nullable ImageData landscapeImageData;
    @Nullable String backgroundHexColor;
    @Nullable List<ActionModel> primaryActions;
    @Nullable Text title;
    @Nullable Text body;
    @Nullable List<ActionModel> secondaryActions;
    @Nullable Button primaryButton;
    @Nullable Button secondaryButton;
    @NonNull IamAnimator.EntryAnimation entryAnimation;
    @NonNull IamAnimator.ExitAnimation exitAnimation;

    public Builder setEntryAnimation(@Nullable IamAnimator.EntryAnimation entryAnimation) {
      this.entryAnimation = entryAnimation;
      return this;
    }

    public Builder setExitAnimation(@Nullable IamAnimator.ExitAnimation exitAnimation) {
      this.exitAnimation = exitAnimation;
      return this;
    }

    public Builder setPortraitImageData(@Nullable ImageData portraitImageData) {
      this.portraitImageData = portraitImageData;
      return this;
    }

    public Builder setLandscapeImageData(@Nullable ImageData landscapeImageData) {
      this.landscapeImageData = landscapeImageData;
      return this;
    }

    public Builder setBackgroundHexColor(@Nullable String backgroundHexColor) {
      this.backgroundHexColor = backgroundHexColor;
      return this;
    }

    public Builder setPrimaryActions(@Nullable List<ActionModel> primaryActions) {
      this.primaryActions = primaryActions;
      return this;
    }

    public Builder setSecondaryActions(@Nullable List<ActionModel> secondaryActions) {
      this.secondaryActions = secondaryActions;
      return this;
    }

    public Builder setTitle(@Nullable Text title) {
      this.title = title;
      return this;
    }

    public Builder setBody(@Nullable Text body) {
      this.body = body;
      return this;
    }

    public Builder setPrimaryButton(@Nullable Button primaryButton) {
      this.primaryButton = primaryButton;
      return this;
    }

    public Builder setSecondaryButton(@Nullable Button secondaryButton) {
      this.secondaryButton = secondaryButton;
      return this;
    }

    public CardMessage build(
            NotificationMetadata notificationMetadata, @NonNull JSONObject data) {
      if (primaryActions == null && primaryActions.size() > 0) {
        throw new IllegalArgumentException("Card model must have a primary action");
      }
      if (primaryButton == null) {
        throw new IllegalArgumentException("Card model must have a primary action button");
      }
      if (secondaryActions != null && secondaryActions.size() > 0 && secondaryButton == null) {
        throw new IllegalArgumentException(
            "Card model secondary action must be null or have a button");
      }
      if (title == null) {
        throw new IllegalArgumentException("Card model must have a title");
      }
      if (portraitImageData == null && landscapeImageData == null) {
        throw new IllegalArgumentException("Card model must have at least one image");
      }
      if (TextUtils.isEmpty(backgroundHexColor)) {
        throw new IllegalArgumentException("Card model must have a background color");
      }

      // We know backgroundColor is not null here because isEmpty checks for null.
      return new CardMessage(
              notificationMetadata,
          title,
          body,
          portraitImageData,
          landscapeImageData,
          backgroundHexColor,
          primaryActions == null ? Collections.emptyList() : primaryActions,
          secondaryActions == null ? Collections.emptyList() : secondaryActions,
          primaryButton,
          secondaryButton,
          entryAnimation,
          exitAnimation,
          data);
    }
  }

  @Override
  public ButtonType getButtonType(List<ActionModel> actions) {
    if (actionsEqual(actions, this.primaryActions)) return ButtonType.PRIMARY;
    if (actionsEqual(actions, this.secondaryActions)) return ButtonType.SECONDARY;
    return ButtonType.UNDEFINED;
  }
}

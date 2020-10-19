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

/** Encapsulates an In App ImageOnly Message. */
public class ImageOnlyMessage extends InAppMessage implements InAppMessage.InAppMessageWithImage {
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  @NonNull private String imageUrl;

  @NonNull private List<ActionModel> actions;

  @NonNull private CloseButtonPosition closeButtonPosition;

  public static ImageOnlyMessage create(NotificationMetadata notificationMetadata, JSONObject payloadJson, JSONObject imageOnlyJson) throws Campaign.InvalidJsonException {
    // Image
    String imageUrlString = imageOnlyJson.optString("imageUrl", null);
    if (TextUtils.isEmpty(imageUrlString)) {
      throw new Campaign.InvalidJsonException("Missing image in imageOnly payload:" + imageOnlyJson.toString());
    }

    // Actions
    List <ActionModel> actions = ActionModel.from(imageOnlyJson.optJSONArray("actions"));

    String closeButtonPositionString = imageOnlyJson.optString("closeButtonPosition", "outside");
    InAppMessage.CloseButtonPosition closeButtonPosition = InAppMessage.CloseButtonPosition.OUTSIDE;
    if ("inside".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.INSIDE;
    if ("none".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.NONE;

    // Animations
    IamAnimator.EntryAnimation entryAnimation = IamAnimator.EntryAnimation.fromSlug(imageOnlyJson.optString("entryAnimation", "fadeIn"));
    IamAnimator.ExitAnimation exitAnimation = IamAnimator.ExitAnimation.fromSlug(imageOnlyJson.optString("exitAnimation", "fadeOut"));
    return new ImageOnlyMessage(notificationMetadata, imageUrlString, actions, closeButtonPosition, entryAnimation, exitAnimation, payloadJson);
  }

  /** @hide */
  @Override
  public int hashCode() {
    int actionHash = actions != null ? actions.hashCode() : 0;
    return imageUrl.hashCode() + actionHash + closeButtonPosition.hashCode() + entryAnimation.hashCode() + exitAnimation.hashCode();
  }

  /** @hide */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true; // same instance
    }
    if (!(o instanceof ImageOnlyMessage)) {
      return false; // not the correct instance type
    }
    ImageOnlyMessage i = (ImageOnlyMessage) o;
    if (entryAnimation != i.entryAnimation) return false;
    if (exitAnimation != i.exitAnimation) return false;
    if (hashCode() != i.hashCode()) {
      return false; // the hashcodes don't match
    }
    if ((actions == null && i.actions != null) || (actions != null && !actions.equals(i.actions))) {
      return false; // the actions don't match
    }

    if (closeButtonPosition != i.closeButtonPosition) return false;

    if (imageUrl.equals(i.imageUrl)) {
      return true; // everything matches
    }
    return false;
  }
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  private ImageOnlyMessage(
      @NonNull NotificationMetadata notificationMetadata,
      @NonNull String imageUrl,
      @NonNull List<ActionModel> actions,
      @NonNull CloseButtonPosition closeButtonPosition,
      @NonNull IamAnimator.EntryAnimation entryAnimation,
      @NonNull IamAnimator.ExitAnimation exitAnimation,
      @NonNull JSONObject data) {
    super(notificationMetadata, MessageType.IMAGE_ONLY, data, entryAnimation, exitAnimation);
    this.imageUrl = imageUrl;
    this.actions = actions;
    this.closeButtonPosition = closeButtonPosition;
  }

  /** Gets the image associated with this message */
  @NonNull
  public String getImageUrl() {
    return imageUrl;
  }

  /** Gets the {@link ActionModel}s associated with this message */
  @NonNull
  public List<ActionModel> getActions() {
    return actions;
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

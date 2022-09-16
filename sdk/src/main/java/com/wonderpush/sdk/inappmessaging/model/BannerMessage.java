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

/** Encapsulates an In App Banner Message. */
public class BannerMessage extends InAppMessage implements InAppMessage.InAppMessageWithImage {
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  @NonNull private final Text title;

  @Nullable private final Text body;
  @Nullable private final String imageUrl;
  @NonNull private final List<ActionModel> actions;
  @NonNull private final String backgroundHexColor;
  @NonNull private final BannerPosition bannerPosition;

  public static BannerMessage create(NotificationMetadata notificationMetadata, JSONObject payloadJson, JSONObject bannerJson) throws Campaign.InvalidJsonException {

    // Title
    Text titleText = Text.fromJSON(bannerJson.optJSONObject("title"));
    if (titleText == null) throw new Campaign.InvalidJsonException("Missing title text");

    // Body
    Text bodyText = Text.fromJSON(bannerJson.optJSONObject("body"));

    // Image
    String imageUrl = JSONUtil.optString(bannerJson, "imageUrl");

    // Background color
    String backgroundHexColor = JSONUtil.optString(bannerJson, "backgroundHexColor", "#FFFFFF");

    String bannerPositionString = JSONUtil.optString(bannerJson, "bannerPosition", "top");
    InAppMessage.BannerPosition bannerPosition = InAppMessage.BannerPosition.TOP;
    if ("bottom".equals(bannerPositionString)) bannerPosition = InAppMessage.BannerPosition.BOTTOM;

    // Action
    List<ActionModel> actions = ActionModel.from(bannerJson.optJSONArray("actions"));

    // Animations
    IamAnimator.EntryAnimation entryAnimation = IamAnimator.EntryAnimation.fromSlug(bannerJson.optString("entryAnimation", "fadeIn"));
    IamAnimator.ExitAnimation exitAnimation = IamAnimator.ExitAnimation.fromSlug(bannerJson.optString("exitAnimation", "fadeOut"));

    return new BannerMessage(notificationMetadata, titleText, bodyText, imageUrl, actions, backgroundHexColor, bannerPosition, entryAnimation, exitAnimation, payloadJson);
  }

  /** @hide */
  @Override
  public int hashCode() {
    int bodyHash = body != null ? body.hashCode() : 0;
    int imageHash = imageUrl != null ? imageUrl.hashCode() : 0;
    int actionHash = actions != null ? actions.hashCode() : 0;
    return title.hashCode() + bodyHash + imageHash + actionHash + backgroundHexColor.hashCode() + bannerPosition.hashCode() + entryAnimation.hashCode() + exitAnimation.hashCode();
  }

  /** @hide */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true; // same instance
    }
    if (!(o instanceof BannerMessage)) {
      return false; // not the correct instance type
    }
    BannerMessage b = (BannerMessage) o;
    if (hashCode() != b.hashCode()) {
      return false; // the hashcodes don't match
    }
    if (bannerPosition != b.bannerPosition) return false;
    if (entryAnimation != b.entryAnimation) return false;
    if (exitAnimation != b.exitAnimation) return false;

    if ((body == null && b.body != null) || (body != null && !body.equals(b.body))) {
      return false; // the bodies don't match
    }
    if ((imageUrl == null && b.imageUrl != null)
        || (imageUrl != null && !imageUrl.equals(b.imageUrl))) {
      return false; // the images don't match
    }
    if ((actions == null && b.actions != null) || (actions != null && !actions.equals(b.actions))) {
      return false; // the actions don't match
    }
    if (!title.equals(b.title)) {
      return false; // the tiles don't match
    }
    if (backgroundHexColor.equals(b.backgroundHexColor)) {
      return true; // everything matches
    }
    return false;
  }
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  private BannerMessage(
      @NonNull NotificationMetadata notificationMetadata,
      @NonNull Text title,
      @Nullable Text body,
      @Nullable String imageUrl,
      List<ActionModel> actions,
      @NonNull String backgroundHexColor,
      @Nullable BannerPosition bannerPosition,
      @NonNull IamAnimator.EntryAnimation entryAnimation,
      @NonNull IamAnimator.ExitAnimation exitAnimation,
      @NonNull JSONObject data) {
    super(notificationMetadata, MessageType.BANNER, data, entryAnimation, exitAnimation);
    this.title = title;
    this.body = body;
    this.imageUrl = imageUrl;
    this.actions = actions;
    this.backgroundHexColor = backgroundHexColor;
    this.bannerPosition = bannerPosition == null ? BannerPosition.TOP : bannerPosition;
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

  /** Gets the URL of the image associated with this message */
  @Nullable
  public String getImageUrl() {
    return imageUrl;
  }

  /** Gets the {@link ActionModel}s associated with this message */
  public List<ActionModel> getActions() {
    return actions;
  }

  /** Gets the {@link com.wonderpush.sdk.inappmessaging.model.InAppMessage.BannerPosition} associated with this message */
  public BannerPosition getBannerPosition() {
    return bannerPosition;
  }
  /** Gets the background hex color associated with this message */
  @NonNull
  public String getBackgroundHexColor() {
    return backgroundHexColor;
  }

  @Override
  public ButtonType getButtonType(List<ActionModel> actions) {
    return actionsEqual(actions, this.actions) ? ButtonType.PRIMARY : ButtonType.UNDEFINED;
  }
}

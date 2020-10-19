package com.wonderpush.sdk.inappmessaging.model;

import android.text.TextUtils;
import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.JSONSerializable;
import com.wonderpush.sdk.NotificationMetadata;
import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import com.wonderpush.sdk.inappmessaging.internal.Logging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Campaign implements JSONSerializable {
  final private JSONObject json;
  final private NotificationMetadata notificationMetadata;
  final private MessagesProto.Content content;
  final private long startTimeMs;
  final private long endTimeMs;
  final private JSONObject segment;
  final private Capping capping;
  final private List<CommonTypesProto.TriggeringCondition> triggeringConditions = new ArrayList<>();
  final private boolean isTestCampaign;


  public static Campaign fromJSON(JSONObject campaignJson) {
    try {
      return new Campaign(campaignJson);
    } catch (InvalidJsonException e) {
      Logging.loge(e.getMessage());
      return null;
    }
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    return new JSONObject(json.toString());
  }


  private Campaign(JSONObject campaignJson) throws InvalidJsonException {
    json = campaignJson;
    isTestCampaign = false;

    JSONObject schedulingJson = campaignJson.optJSONObject("scheduling");

    // Scheduling
    if (schedulingJson == null) {
      throw new InvalidJsonException("Missing scheduling in campaign payload: " + campaignJson.toString());
    }

    startTimeMs = schedulingJson.optLong("startDate");
    endTimeMs = schedulingJson.optLong("endDate");

    // Segmentation
    segment = campaignJson.optJSONObject("segment");

    // Capping
    JSONObject cappingJson = campaignJson.optJSONObject("capping");
    long maxImpressions = cappingJson != null ? cappingJson.optLong("maxImpressions", 1) : 1;
    long snoozeTime = cappingJson != null ? cappingJson.optLong("snoozeTime", 0) : 0;
    capping = new Capping(maxImpressions, snoozeTime);

    // Notifications
    JSONArray notificationsJson = campaignJson.optJSONArray("notifications");
    if (notificationsJson == null) {
      throw new InvalidJsonException("Missing notifications entry in campaign payload: " + campaignJson.toString());
    }
    JSONObject notificationJson = notificationsJson.length() > 0 ? notificationsJson.optJSONObject(0) : null;
    if (notificationJson == null) {
      throw new InvalidJsonException("Missing notification entry in campaign payload: " + campaignJson.toString());
    }

    content = new MessagesProto.Content();
    content.setDataBundle(notificationJson.optJSONObject("payload"));

    JSONObject reportingJson = notificationJson.optJSONObject("reporting");
    if (reportingJson == null) {
      throw new InvalidJsonException("Missing reporting in notification payload: " + notificationJson.toString());
    }
    JSONObject contentJson = notificationJson.optJSONObject("content");
    if (contentJson == null) {
      throw new InvalidJsonException("Missing content in notification payload: " + notificationJson.toString());
    }
    JSONArray triggersJson = campaignJson.optJSONArray("triggers");
    if (triggersJson == null || triggersJson.length() < 1) {
      throw new InvalidJsonException("Missing triggers in notification payload: " + notificationJson.toString());
    }

    String campaignId = reportingJson.optString("campaignId");
    if (campaignId == null) {
      throw new InvalidJsonException("Missing campaignId in reporting payload: " + reportingJson.toString());
    }
    String viewId = reportingJson.optString("viewId");
    String notificationId = reportingJson.optString("notificationId");
    if (notificationId == null) {
      throw new InvalidJsonException("Missing notificationId in reporting payload: " + reportingJson.toString());
    }
    notificationMetadata = new NotificationMetadata(campaignId, notificationId, viewId, false);

    JSONObject cardJson = contentJson.optJSONObject("card");
    JSONObject bannerJson = contentJson.optJSONObject("banner");
    JSONObject modalJson = contentJson.optJSONObject("modal");
    JSONObject imageOnlyJson = contentJson.optJSONObject("imageOnly");

    if (cardJson != null) {
      MessagesProto.CardMessage message = new MessagesProto.CardMessage();
      content.setCard(message);

      // Title
      JSONObject titleJson = cardJson.optJSONObject("title");
      if (null != titleJson) {
        MessagesProto.Text titleText = MessagesProto.Text.fromJSON(titleJson);
        if (titleText == null) {
          throw new InvalidJsonException("Missing title in card payload:" + cardJson.toString());
        }
        message.setTitle(titleText);
      }

      // Body
      JSONObject bodyJson = cardJson.optJSONObject("body");
      if (null != bodyJson) {
        MessagesProto.Text bodyText = MessagesProto.Text.fromJSON(bodyJson);
        message.setBody(bodyText);
      }

      // Images
      message.setPortraitImageUrl(cardJson.optString("portraitImageUrl"));
      message.setLandscapeImageUrl(cardJson.optString("landscapeImageUrl"));

      // Background (mandatory)
      message.setBackgroundHexColor(cardJson.optString("backgroundHexColor", "#FFFFFF"));

      // Actions & buttons
      message.setPrimaryActionButton(MessagesProto.Button.fromJSON(cardJson.optJSONObject("primaryActionButton")));
      message.setSecondaryActionButton(MessagesProto.Button.fromJSON(cardJson.optJSONObject("secondaryActionButton")));
      message.setPrimaryActions(ActionModel.from(cardJson.optJSONArray("primaryActions")));
      message.setSecondaryActions(ActionModel.from(cardJson.optJSONArray("secondaryActions")));

      // Animations
      message.setEntryAnimation(IamAnimator.EntryAnimation.fromSlug(cardJson.optString("entryAnimation", "fadeIn")));
      message.setExitAnimation(IamAnimator.ExitAnimation.fromSlug(cardJson.optString("exitAnimation", "fadeOut")));
    } else if (bannerJson != null) {
      MessagesProto.BannerMessage message = new MessagesProto.BannerMessage();
      content.setBanner(message);

      // Title
      JSONObject titleJson = bannerJson.optJSONObject("title");
      if (null != titleJson) {
        MessagesProto.Text titleText = MessagesProto.Text.fromJSON(titleJson);
        if (titleText == null) {
          throw new InvalidJsonException("Missing title in banner payload:" + bannerJson.toString());
        }
        message.setTitle(titleText);
      }

      // Body
      JSONObject bodyJson = bannerJson.optJSONObject("body");
      if (null != bodyJson) {
        MessagesProto.Text bodyText = MessagesProto.Text.fromJSON(bodyJson);
        message.setBody(bodyText);
      }

      // Image
      message.setImageUrl(bannerJson.optString("imageUrl"));

      // Background color
      message.setBackgroundHexColor(bannerJson.optString("backgroundHexColor", "#FFFFFF"));

      String bannerPositionString = bannerJson.optString("bannerPosition", "top");
      InAppMessage.BannerPosition bannerPosition = InAppMessage.BannerPosition.TOP;
      if ("bottom".equals(bannerPositionString)) bannerPosition = InAppMessage.BannerPosition.BOTTOM;
      message.setBannerPosition(bannerPosition);

      // Action
      message.setActions(ActionModel.from(bannerJson.optJSONArray("actions")));

      // Animations
      message.setEntryAnimation(IamAnimator.EntryAnimation.fromSlug(bannerJson.optString("entryAnimation", "fadeIn")));
      message.setExitAnimation(IamAnimator.ExitAnimation.fromSlug(bannerJson.optString("exitAnimation", "fadeOut")));
    } else if (modalJson != null) {
      MessagesProto.ModalMessage message = new MessagesProto.ModalMessage();
      content.setModal(message);

      // Title
      JSONObject titleJson = modalJson.optJSONObject("title");
      if (null != titleJson) {
        MessagesProto.Text titleText = MessagesProto.Text.fromJSON(titleJson);
        if (titleText == null) {
          throw new InvalidJsonException("Missing title in modal payload:" + modalJson.toString());
        }
        message.setTitle(titleText);
      }

      // Body
      JSONObject bodyJson = modalJson.optJSONObject("body");
      if (null != bodyJson) {
        MessagesProto.Text bodyText = MessagesProto.Text.fromJSON(bodyJson);
        message.setBody(bodyText);
      }

      // Image
      message.setImageUrl(modalJson.optString("imageUrl"));

      // Background color
      message.setBackgroundHexColor(modalJson.optString("backgroundHexColor", "#FFFFFF"));

      // Action & button
      message.setActionButton(MessagesProto.Button.fromJSON(modalJson.optJSONObject("actionButton")));
      message.setActions(ActionModel.from(modalJson.optJSONArray("actions")));

      String closeButtonPositionString = modalJson.optString("closeButtonPosition", "outside");
      InAppMessage.CloseButtonPosition closeButtonPosition = InAppMessage.CloseButtonPosition.OUTSIDE;
      if ("inside".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.INSIDE;
      if ("none".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.NONE;
      message.setCloseButtonPosition(closeButtonPosition);

      // Animations
      message.setEntryAnimation(IamAnimator.EntryAnimation.fromSlug(modalJson.optString("entryAnimation", "fadeIn")));
      message.setExitAnimation(IamAnimator.ExitAnimation.fromSlug(modalJson.optString("exitAnimation", "fadeOut")));

    } else if (imageOnlyJson != null) {
      MessagesProto.ImageOnlyMessage message = new MessagesProto.ImageOnlyMessage();
      content.setImageOnly(message);

      // Image
      String imageUrlString = imageOnlyJson.optString("imageUrl");
      if (TextUtils.isEmpty(imageUrlString)) {
        throw new InvalidJsonException("Missing image in imageOnly payload:" + imageOnlyJson.toString());
      }
      message.setImageUrl(imageUrlString);

      // Actions
      message.setActions(ActionModel.from(imageOnlyJson.optJSONArray("actions")));

      String closeButtonPositionString = imageOnlyJson.optString("closeButtonPosition", "outside");
      InAppMessage.CloseButtonPosition closeButtonPosition = InAppMessage.CloseButtonPosition.OUTSIDE;
      if ("inside".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.INSIDE;
      if ("none".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.NONE;
      message.setCloseButtonPosition(closeButtonPosition);

      // Animations
      message.setEntryAnimation(IamAnimator.EntryAnimation.fromSlug(imageOnlyJson.optString("entryAnimation", "fadeIn")));
      message.setExitAnimation(IamAnimator.ExitAnimation.fromSlug(imageOnlyJson.optString("exitAnimation", "fadeOut")));

    } else {
      throw new InvalidJsonException("Unknown message type in message node: " + contentJson.toString());
    }

    // Triggers
    for (int i = 0; i < triggersJson.length(); i++) {
      JSONObject triggerJson = triggersJson.optJSONObject(i);
      if (triggerJson == null) continue;
      CommonTypesProto.TriggeringCondition triggeringCondition = CommonTypesProto.TriggeringCondition.fromJSON(triggerJson);
      if (triggeringCondition != null) triggeringConditions.add(triggeringCondition);
    }

    if (triggeringConditions.size() < 1) {
      throw new InvalidJsonException("No triggers in campaign" + campaignJson.toString());
    }
  }

  public boolean isTestCampaign() {
    return isTestCampaign;
  }

  /**
   * <pre>
   * Start time of the campaign - we use long vs Timestamp here to
   * minimize the size of the bundles sent back to the sdk clients
   * </pre>
   *
   * <code>optional int64 campaign_start_time_millis = 3;</code>
   */
  public long getCampaignStartTimeMillis() {
    return startTimeMs;
  }

  /**
   * <pre>
   * [optional]  end time of the campaign - we use long vs Timestamp here to
   * minimize the size of the bundles sent back to the sdk clients
   * </pre>
   *
   * <code>optional int64 campaign_end_time_millis = 4;</code>
   */
  public long getCampaignEndTimeMillis() {
    return endTimeMs;
  }

  public NotificationMetadata getNotificationMetadata() {
    return notificationMetadata;
  }

  public MessagesProto.Content getContent() {
    return content;
  }

  public JSONObject getSegment() {
    return segment;
  }

  public Capping getCapping() {
    return capping;
  }

  public List<CommonTypesProto.TriggeringCondition> getTriggeringConditions() {
    return new ArrayList<>(triggeringConditions);
  }

  public static final class Capping {
    final private long snoozeTime;
    final private long maxImpressions;

    public Capping(long maxImpressions, long snoozeTime) {
      this.maxImpressions = maxImpressions;
      this.snoozeTime = snoozeTime;
    }

    public long getSnoozeTime() {
      return snoozeTime;
    }

    public long getMaxImpressions() {
      return maxImpressions;
    }

  }

  private static class InvalidJsonException extends Exception {
    InvalidJsonException(String msg) {
      super(msg);
    }
  }

}

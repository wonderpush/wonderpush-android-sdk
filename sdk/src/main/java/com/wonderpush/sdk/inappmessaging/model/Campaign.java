package com.wonderpush.sdk.inappmessaging.model;

import com.wonderpush.sdk.JSONSerializable;
import com.wonderpush.sdk.JSONUtil;
import com.wonderpush.sdk.NotificationMetadata;
import com.wonderpush.sdk.inappmessaging.internal.Logging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Campaign implements JSONSerializable {
  final private JSONObject json;
  final private NotificationMetadata notificationMetadata;
  final private InAppMessage content;
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

    JSONObject payloadJson = notificationJson.optJSONObject("payload");

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

    String campaignId = JSONUtil.optString(reportingJson, "campaignId");
    if (campaignId == null) {
      throw new InvalidJsonException("Missing campaignId in reporting payload: " + reportingJson.toString());
    }
    String viewId = JSONUtil.optString(reportingJson, "viewId");
    String notificationId = JSONUtil.optString(reportingJson, "notificationId");
    if (notificationId == null) {
      throw new InvalidJsonException("Missing notificationId in reporting payload: " + reportingJson.toString());
    }
    notificationMetadata = new NotificationMetadata(campaignId, notificationId, viewId, false);
    content = parseContent(notificationMetadata, payloadJson, contentJson);
    if (content == null) {
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

  public static InAppMessage parseContent(NotificationMetadata notificationMetadata, JSONObject payloadJson, JSONObject contentJson) throws InvalidJsonException {
    JSONObject cardJson = contentJson.optJSONObject("card");
    JSONObject bannerJson = contentJson.optJSONObject("banner");
    JSONObject modalJson = contentJson.optJSONObject("modal");
    JSONObject imageOnlyJson = contentJson.optJSONObject("imageOnly");

    if (cardJson != null) {
      return CardMessage.create(notificationMetadata, payloadJson, cardJson);
    } else if (bannerJson != null) {
      return BannerMessage.create(notificationMetadata, payloadJson, bannerJson);
    } else if (modalJson != null) {
      return ModalMessage.create(notificationMetadata, payloadJson, modalJson);
    } else if (imageOnlyJson != null) {
      return ImageOnlyMessage.create(notificationMetadata, payloadJson, imageOnlyJson);
    }
    return null;
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

  public InAppMessage getContent() {
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

  static class InvalidJsonException extends Exception {
    InvalidJsonException(String msg) {
      super(msg);
    }
  }

}

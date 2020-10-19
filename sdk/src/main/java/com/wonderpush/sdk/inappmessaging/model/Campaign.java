
package com.wonderpush.sdk.inappmessaging.model;

import android.text.TextUtils;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.JSONSerializable;
import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import com.wonderpush.sdk.inappmessaging.internal.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Campaign {
  private Campaign() {}

  public static final class Capping {
    private long snoozeTime;
    private long maxImpressions;

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
  /**
   * <pre>
   * The 'thick' message that gets sent to clients
   * </pre>
   *
   */
  public  static final class ThickContent implements JSONSerializable {
    private JSONObject json;
    private Payload payload_;
    private MessagesProto.Content content_;
    private CommonTypesProto.Priority priority_;
    private boolean isTestCampaign_;
    private JSONObject segment_;
    private Capping capping_;
    private List<CommonTypesProto.TriggeringCondition> triggeringConditions_ = new ArrayList<>();

    @Override
    public JSONObject toJSON() throws JSONException {
      return new JSONObject(json.toString());
    }

    private static class InvalidJsonException extends Exception {
      InvalidJsonException(String msg) {
        super(msg);
      }
    }

    private ThickContent(JSONObject campaignJson) throws InvalidJsonException {
      this.json = campaignJson;
      // FIXME: get rid of the priority
      CommonTypesProto.Priority priority = new CommonTypesProto.Priority();
      priority.setValue(1);
      this.setPriority(priority);
      Payload payload = new Payload();
      this.setPayload(payload);

      JSONObject schedulingJson = campaignJson.optJSONObject("scheduling");

      // Scheduling
      if (schedulingJson == null) {
        throw new InvalidJsonException("Missing scheduling in campaign payload: " + campaignJson.toString());
      }

      Long startDate = schedulingJson.optLong("startDate");
      if (startDate != 0L) payload.setCampaignStartTimeMillis(startDate);
      Long endDate = schedulingJson.optLong("endDate");
      if (endDate != 0L) payload.setCampaignEndTimeMillis(endDate);

      // Segmentation
      JSONObject segmentJson = campaignJson.optJSONObject("segment");
      if (segmentJson != null) {
        setSegment(segmentJson);
      }

      // Capping
      JSONObject cappingJson = campaignJson.optJSONObject("capping");
      long maxImpressions = cappingJson != null ? cappingJson.optLong("maxImpressions", 1) : 1;
      long snoozeTime = cappingJson != null ? cappingJson.optLong("snoozeTime", 0) : 0;
      setCapping(new Capping(maxImpressions, snoozeTime));

      // Notifications
      JSONArray notificationsJson = campaignJson.optJSONArray("notifications");
      if (notificationsJson == null) {
        throw new InvalidJsonException("Missing notifications entry in campaign payload: " + campaignJson.toString());
      }
      JSONObject notificationJson = notificationsJson.length() > 0 ? notificationsJson.optJSONObject(0) : null;
      if (notificationJson == null) {
        throw new InvalidJsonException("Missing notification entry in campaign payload: " + campaignJson.toString());
      }

      MessagesProto.Content content = new MessagesProto.Content();
      content.setDataBundle(notificationJson.optJSONObject("payload"));
      this.setContent(content);
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
      payload.setCampaignId(campaignId);
      payload.setViewId(viewId);
      payload.setNotificationId(notificationId);

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
        if ("top".equals(bannerPositionString)) bannerPosition = InAppMessage.BannerPosition.TOP;
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
        if ("outside".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.OUTSIDE;
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
        if ("outside".equals(closeButtonPositionString)) closeButtonPosition = InAppMessage.CloseButtonPosition.OUTSIDE;
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
        if (triggeringCondition != null) this.addTriggeringConditions(triggeringCondition);
      }

      if (this.getTriggeringConditionsList().size() < 1) {
        throw new InvalidJsonException("No triggers in campaign" + campaignJson.toString());
      }
    }

    public static ThickContent fromJSON(JSONObject campaignJson) {
      try {
        return new ThickContent(campaignJson);
      } catch (InvalidJsonException e) {
        Logging.loge(e.getMessage());
        return null;
      }
    }


    public void clearPayload() {
      payload_ = null;
    }

    public Payload getPayload() {
      return payload_;
    }

    private void setPayload(Payload value) {
      if (value == null) {
        throw new NullPointerException();
      }
      payload_ = value;
    }

    /**
     * <pre>
     * Content
     * </pre>
     *
     * <code>optional .inappmessaging.Content content = 3;</code>
     */
    public boolean hasContent() {
      return content_ != null;
    }
    /**
     * <pre>
     * Content
     * </pre>
     *
     * <code>optional .inappmessaging.Content content = 3;</code>
     */
    public MessagesProto.Content getContent() {
      return content_;
    }
    /**
     * <pre>
     * Content
     * </pre>
     *
     * <code>optional .inappmessaging.Content content = 3;</code>
     */
    private void setContent(MessagesProto.Content value) {
      content_ = value;

    }

    public JSONObject getSegment() {
      return segment_;
    }

    private void setSegment(JSONObject segment) {
      segment_ = segment;
    }

    public Capping getCapping() {
      return capping_;
    }

    public void setCapping(Capping capping) {
      this.capping_ = capping;
    }

    /**
     * <pre>
     * Priority of the campaign/message
     * If two messages have the same priority, the one from the
     * most-recently-started campaign will 'win'
     * </pre>
     *
     * <code>optional .inappmessaging.Priority priority = 4;</code>
     */
    public boolean hasPriority() {
      return priority_ != null;
    }
    /**
     * <pre>
     * Priority of the campaign/message
     * If two messages have the same priority, the one from the
     * most-recently-started campaign will 'win'
     * </pre>
     *
     * <code>optional .inappmessaging.Priority priority = 4;</code>
     */
    public CommonTypesProto.Priority getPriority() {
      return priority_;
    }
    /**
     * <pre>
     * Priority of the campaign/message
     * If two messages have the same priority, the one from the
     * most-recently-started campaign will 'win'
     * </pre>
     *
     * <code>optional .inappmessaging.Priority priority = 4;</code>
     */
    private void setPriority(CommonTypesProto.Priority value) {
      priority_ = value;

      }

    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    public java.util.List<CommonTypesProto.TriggeringCondition> getTriggeringConditionsList() {
      return triggeringConditions_;
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    public java.util.List<? extends CommonTypesProto.TriggeringCondition>
        getTriggeringConditionsOrBuilderList() {
      return triggeringConditions_;
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    public int getTriggeringConditionsCount() {
      return triggeringConditions_.size();
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    public CommonTypesProto.TriggeringCondition getTriggeringConditions(int index) {
      return triggeringConditions_.get(index);
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    public CommonTypesProto.TriggeringCondition getTriggeringConditionsOrBuilder(
        int index) {
      return triggeringConditions_.get(index);
    }

    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    private void setTriggeringConditions(
        int index, CommonTypesProto.TriggeringCondition value) {
      triggeringConditions_.set(index, value);
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    private void addTriggeringConditions(CommonTypesProto.TriggeringCondition value) {
      if (value == null) {
        throw new NullPointerException();
      }
      triggeringConditions_.add(value);
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    private void addTriggeringConditions(
        int index, CommonTypesProto.TriggeringCondition value) {
      if (value == null) {
        throw new NullPointerException();
      }
      triggeringConditions_.add(index, value);
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    private void addAllTriggeringConditions(
        Collection<? extends CommonTypesProto.TriggeringCondition> values) {
      triggeringConditions_.addAll(values);
    }
    /**
     * <pre>
     * condition to trigger the IAM
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggeringCondition triggering_conditions = 5;</code>
     */
    private void removeTriggeringConditions(int index) {
      triggeringConditions_.remove(index);
    }

    /**
     * <pre>
     * if true, it's a campaign that's to be tested on client side. otherwise, it
     * it's a regular message to be rendered.
     * </pre>
     *
     * <code>optional bool is_test_campaign = 7;</code>
     */
    public boolean getIsTestCampaign() {
      return isTestCampaign_;
    }
    /**
     * <pre>
     * if true, it's a campaign that's to be tested on client side. otherwise, it
     * it's a regular message to be rendered.
     * </pre>
     *
     * <code>optional bool is_test_campaign = 7;</code>
     */
    private void setIsTestCampaign(boolean value) {

      isTestCampaign_ = value;
    }

  }

  public  static final class Payload {
    private String campaignId_;
    private String viewId_;
    private String notificationId_;
    private String experimentalCampaignId_;
    private long campaignStartTimeMillis_;
    private long campaignEndTimeMillis_;

    private Payload() {
    }

    /**
     * <pre>
     * Campaign id
     * </pre>
     *
     * <code>optional string campaign_id = 1;</code>
     */
    public String getCampaignId() {
      return campaignId_;
    }
    /**
     * <pre>
     * Campaign id
     * </pre>
     *
     * <code>optional string campaign_id = 1;</code>
     */
    private void setCampaignId(
        String value) {

      campaignId_ = value;
    }

    public String getViewId() {
      return viewId_;
    }

    private void setViewId(String viewId_) {
      this.viewId_ = viewId_;
    }

    public String getNotificationId() {
      return notificationId_;
    }

    private void setNotificationId(String notificationId_) {
      this.notificationId_ = notificationId_;
    }

      /**
     * <pre>
     * [optional] if the campaign is the result of a rolled out campaign,
     * the old experimental campaign id here is used for impression tracking on
     * the client
     * </pre>
     *
     * <code>optional string experimental_campaign_id = 2;</code>
     */
    public String getExperimentalCampaignId() {
      return experimentalCampaignId_;
    }
    /**
     * <pre>
     * [optional] if the campaign is the result of a rolled out campaign,
     * the old experimental campaign id here is used for impression tracking on
     * the client
     * </pre>
     *
     * <code>optional string experimental_campaign_id = 2;</code>
     */
    private void setExperimentalCampaignId(
        String value) {
      experimentalCampaignId_ = value;
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
      return campaignStartTimeMillis_;
    }
    /**
     * <pre>
     * Start time of the campaign - we use long vs Timestamp here to
     * minimize the size of the bundles sent back to the sdk clients
     * </pre>
     *
     * <code>optional int64 campaign_start_time_millis = 3;</code>
     */
    private void setCampaignStartTimeMillis(long value) {

      campaignStartTimeMillis_ = value;
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
      return campaignEndTimeMillis_;
    }
    /**
     * <pre>
     * [optional]  end time of the campaign - we use long vs Timestamp here to
     * minimize the size of the bundles sent back to the sdk clients
     * </pre>
     *
     * <code>optional int64 campaign_end_time_millis = 4;</code>
     */
    private void setCampaignEndTimeMillis(long value) {

      campaignEndTimeMillis_ = value;
    }
    /**
     * <pre>
     * [optional]  end time of the campaign - we use long vs Timestamp here to
     * minimize the size of the bundles sent back to the sdk clients
     * </pre>
     *
     * <code>optional int64 campaign_end_time_millis = 4;</code>
     */
    private void clearCampaignEndTimeMillis() {

      campaignEndTimeMillis_ = 0L;
    }

  }
}

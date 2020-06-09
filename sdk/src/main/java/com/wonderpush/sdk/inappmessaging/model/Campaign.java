
package com.wonderpush.sdk.inappmessaging.model;

import android.text.TextUtils;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.JSONSerializable;
import com.wonderpush.sdk.inappmessaging.internal.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Campaign {
  private Campaign() {}

  /**
   * <pre>
   * The 'thick' message that gets sent to clients
   * </pre>
   *
   */
  public  static final class ThickContent implements JSONSerializable {
    private JSONObject json;
    private int payloadCase_ = 0;
    private Object payload_;
    private MessagesProto.Content content_;
    private CommonTypesProto.Priority priority_;
    private boolean isTestCampaign_;
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
      Campaign.VanillaCampaignPayload vanillaContent = new Campaign.VanillaCampaignPayload();
      this.setVanillaPayload(vanillaContent);

      JSONObject schedulingJson = campaignJson.optJSONObject("scheduling");

      // Scheduling
      if (schedulingJson == null) {
        throw new InvalidJsonException("Missing scheduling in campaign payload: " + campaignJson.toString());
      }

      Long startDate = schedulingJson.optLong("startDate");
      if (startDate != 0L) vanillaContent.setCampaignStartTimeMillis(startDate);
      Long endDate = schedulingJson.optLong("endDate");
      if (endDate != 0L) vanillaContent.setCampaignEndTimeMillis(endDate);

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
      vanillaContent.setCampaignId(campaignId);
      vanillaContent.setViewId(viewId);
      vanillaContent.setNotificationId(notificationId);

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

        // Action
        message.setActions(ActionModel.from(bannerJson.optJSONArray("actions")));
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

    public enum PayloadCase {
      VANILLA_PAYLOAD(1),
      EXPERIMENTAL_PAYLOAD(2),
      PAYLOAD_NOT_SET(0);
      private final int value;
      private PayloadCase(int value) {
        this.value = value;
      }
      /**
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @Deprecated
      public static PayloadCase valueOf(int value) {
        return forNumber(value);
      }

      public static PayloadCase forNumber(int value) {
        switch (value) {
          case 1: return VANILLA_PAYLOAD;
          case 2: return EXPERIMENTAL_PAYLOAD;
          case 0: return PAYLOAD_NOT_SET;
          default: return null;
        }
      }
      public int getNumber() {
        return this.value;
      }
    };

    public PayloadCase
    getPayloadCase() {
      return PayloadCase.forNumber(
          payloadCase_);
    }

    public void clearPayload() {
      payloadCase_ = 0;
      payload_ = null;
    }

    /**
     * <code>optional .inappmessaging.v1.VanillaCampaignPayload vanilla_payload = 1;</code>
     */
    public Campaign.VanillaCampaignPayload getVanillaPayload() {
      if (payloadCase_ == 1) {
         return (Campaign.VanillaCampaignPayload) payload_;
      }
      return null;
    }
    /**
     * <code>optional .inappmessaging.v1.VanillaCampaignPayload vanilla_payload = 1;</code>
     */
    private void setVanillaPayload(Campaign.VanillaCampaignPayload value) {
      if (value == null) {
        throw new NullPointerException();
      }
      payload_ = value;
      payloadCase_ = 1;
    }

    /**
     * <code>optional .inappmessaging.v1.ExperimentalCampaignPayload experimental_payload = 2;</code>
     */
    public Campaign.ExperimentalCampaignPayload getExperimentalPayload() {
      if (payloadCase_ == 2) {
         return (Campaign.ExperimentalCampaignPayload) payload_;
      }
      return null;
    }
    /**
     * <code>optional .inappmessaging.v1.ExperimentalCampaignPayload experimental_payload = 2;</code>
     */
    private void setExperimentalPayload(Campaign.ExperimentalCampaignPayload value) {
      if (value == null) {
        throw new NullPointerException();
      }
      payload_ = value;
      payloadCase_ = 2;
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

  /**
   * Protobuf type {@code google.internal.inappmessaging.v1.VanillaCampaignPayload}
   */
  public  static final class VanillaCampaignPayload {
    private String campaignId_;
    private String viewId_;
    private String notificationId_;
    private String experimentalCampaignId_;
    private long campaignStartTimeMillis_;
    private long campaignEndTimeMillis_;

    private VanillaCampaignPayload() {
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

  /**
   * Protobuf type {@code google.internal.inappmessaging.v1.ExperimentalCampaignPayload}
   */
  public  static final class ExperimentalCampaignPayload {
    private String campaignId_;
    private long campaignStartTimeMillis_;
    private long campaignEndTimeMillis_;

    private ExperimentalCampaignPayload() {
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

    /**
     * <pre>
     * IAM configured start time of the experiment - we use long vs Timestamp
     * here to minimize the size of the bundles sent back to the sdk clients
     * </pre>
     *
     * <code>optional int64 campaign_start_time_millis = 3;</code>
     */
    public long getCampaignStartTimeMillis() {
      return campaignStartTimeMillis_;
    }
    /**
     * <pre>
     * IAM configured start time of the experiment - we use long vs Timestamp
     * here to minimize the size of the bundles sent back to the sdk clients
     * </pre>
     *
     * <code>optional int64 campaign_start_time_millis = 3;</code>
     */
    private void setCampaignStartTimeMillis(long value) {

      campaignStartTimeMillis_ = value;
    }
    /**
     * <pre>
     * IAM configured start time of the experiment - we use long vs Timestamp
     * here to minimize the size of the bundles sent back to the sdk clients
     * </pre>
     *
     * <code>optional int64 campaign_start_time_millis = 3;</code>
     */
    private void clearCampaignStartTimeMillis() {

      campaignStartTimeMillis_ = 0L;
    }

    /**
     * <pre>
     * IAM configured end time of the experiment - we use long vs Timestamp here
     * to minimize the size of the bundles sent back to the sdk clients. Note that
     * for an experiment the end time is defined by whichever is earliest of
     * (campaign end time, experiment time to live)
     * </pre>
     *
     * <code>optional int64 campaign_end_time_millis = 4;</code>
     */
    public long getCampaignEndTimeMillis() {
      return campaignEndTimeMillis_;
    }
    /**
     * <pre>
     * IAM configured end time of the experiment - we use long vs Timestamp here
     * to minimize the size of the bundles sent back to the sdk clients. Note that
     * for an experiment the end time is defined by whichever is earliest of
     * (campaign end time, experiment time to live)
     * </pre>
     *
     * <code>optional int64 campaign_end_time_millis = 4;</code>
     */
    private void setCampaignEndTimeMillis(long value) {

      campaignEndTimeMillis_ = value;
    }
    /**
     * <pre>
     * IAM configured end time of the experiment - we use long vs Timestamp here
     * to minimize the size of the bundles sent back to the sdk clients. Note that
     * for an experiment the end time is defined by whichever is earliest of
     * (campaign end time, experiment time to live)
     * </pre>
     *
     * <code>optional int64 campaign_end_time_millis = 4;</code>
     */
    private void clearCampaignEndTimeMillis() {

      campaignEndTimeMillis_ = 0L;
    }

  }

}
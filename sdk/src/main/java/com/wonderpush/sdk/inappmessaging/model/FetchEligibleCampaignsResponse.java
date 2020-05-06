
package com.wonderpush.sdk.inappmessaging.model;

import com.wonderpush.sdk.JSONDeserializable;
import com.wonderpush.sdk.JSONSerializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public  final class FetchEligibleCampaignsResponse implements JSONSerializable, JSONDeserializable {
  private List<Campaign.ThickContent> messages_ = new ArrayList<>();
  private long expirationEpochTimestampMillis_;

  public FetchEligibleCampaignsResponse() {
  }

  public static FetchEligibleCampaignsResponse buildFromJSON(JSONObject json) {
    FetchEligibleCampaignsResponse result = new FetchEligibleCampaignsResponse();
    result.fromJSON(json);
    return result;
  }

  @Override
  public void fromJSON(JSONObject json) {
    JSONArray campaignsJson = json.optJSONArray("campaigns");
    if (campaignsJson == null) return;

    for (int i = 0; i < campaignsJson.length(); i++) {
      JSONObject campaignJson = campaignsJson.optJSONObject(i);
      if (campaignJson == null) continue;
      Campaign.ThickContent thickContent = Campaign.ThickContent.fromJSON(campaignJson);
      if (thickContent != null) messages_.add(thickContent);
    }
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject result = new JSONObject();
    JSONArray campaigns = new JSONArray();
    result.put("campaigns", campaigns);
    for (Campaign.ThickContent campaign : messages_) campaigns.put(campaign.toJSON());
    return result;
  }

  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  public java.util.List<Campaign.ThickContent> getMessagesList() {
    return messages_;
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  public java.util.List<? extends Campaign.ThickContent>
      getMessagesOrBuilderList() {
    return messages_;
  }

  public void addMessage(Campaign.ThickContent message) {
    messages_.add(message);
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  public int getMessagesCount() {
    return messages_.size();
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  public Campaign.ThickContent getMessages(int index) {
    return messages_.get(index);
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  public Campaign.ThickContent getMessagesOrBuilder(
      int index) {
    return messages_.get(index);
  }

  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  private void setMessages(
      int index, Campaign.ThickContent value) {
    if (value == null) {
      throw new NullPointerException();
    }
    messages_.set(index, value);
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  private void addMessages(Campaign.ThickContent value) {
    messages_.add(value);
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  private void addMessages(
      int index, Campaign.ThickContent value) {
    messages_.add(index, value);
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  private void addAllMessages(
      Collection<? extends Campaign.ThickContent> values) {
    messages_.addAll(values);
  }
  /**
   * <pre>
   * list of eligible messages sorted in high-to-low priority order
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.ThickContent messages = 1;</code>
   */
  private void removeMessages(int index) {
    messages_.remove(index);
  }

  /**
   * <pre>
   * epoch time at which time clients must invalidates their cache
   * </pre>
   *
   * <code>optional int64 expiration_epoch_timestamp_millis = 2;</code>
   */
  public long getExpirationEpochTimestampMillis() {
    return expirationEpochTimestampMillis_;
  }
  /**
   * <pre>
   * epoch time at which time clients must invalidates their cache
   * </pre>
   *
   * <code>optional int64 expiration_epoch_timestamp_millis = 2;</code>
   */
  public void setExpirationEpochTimestampMillis(long value) {

    expirationEpochTimestampMillis_ = value;
  }

}

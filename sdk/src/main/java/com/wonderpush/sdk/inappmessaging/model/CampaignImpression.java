package com.wonderpush.sdk.inappmessaging.model;

import com.wonderpush.sdk.JSONDeserializable;
import com.wonderpush.sdk.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <pre>
 * A campaign id and timestamp noting when the device displayed this campaign
 * </pre>
 *
 * Protobuf type {@code inappmessaging.v1.sdkserving.CampaignImpression}
 */
public  final class CampaignImpression implements JSONSerializable, JSONDeserializable {
  private String campaignId_;
  private long impressionTimestampMillis_;
  private long impressionCount_;

  public CampaignImpression() {
  }

  /**
   * <pre>
   * [required] campaign_id
   * </pre>
   *
   * <code>optional string campaign_id = 1;</code>
   */
  public String getCampaignId() {
    return campaignId_;
  }
  /**
   * <pre>
   * [required] campaign_id
   * </pre>
   *
   * <code>optional string campaign_id = 1;</code>
   */
  public void setCampaignId(
      String value) {
    campaignId_ = value;
  }

  /**
   * <pre>
   * [required] when instance last displayed the content for this campaign
   * </pre>
   *
   * <code>optional int64 impression_timestamp_millis = 2;</code>
   */
  public long getImpressionTimestampMillis() {
    return impressionTimestampMillis_;
  }
  /**
   * <pre>
   * [required] when instance last displayed the content for this campaign
   * </pre>
   *
   * <code>optional int64 impression_timestamp_millis = 2;</code>
   */
  public void setImpressionTimestampMillis(long value) {
    impressionTimestampMillis_ = value;
  }

  public long getImpressionCount() {
    return impressionCount_;
  }

  public void setImpressionCount(long impressionCount) {
    this.impressionCount_ = impressionCount;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject result = new JSONObject();
    if (campaignId_ != null) result.put("campaignId", campaignId_);
    result.put("impressionTimestampMillis", impressionTimestampMillis_);
    result.put("impressionCount", impressionCount_);
    return result;
  }

  @Override
  public void fromJSON(JSONObject json) {
    if (json == null) return;
    campaignId_ = json.optString("campaignId");
    impressionTimestampMillis_ = json.optLong("impressionTimestampMillis");
    impressionCount_ = json.optLong("impressionCount", 0);
  }
}


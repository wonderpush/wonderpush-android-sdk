package com.wonderpush.sdk.inappmessaging.model;

import com.wonderpush.sdk.JSONDeserializable;
import com.wonderpush.sdk.JSONSerializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <pre>
 * Proto definition to facilitate storing impressions on the client
 * </pre>
 *
 * Protobuf type {@code inappmessaging.v1.sdkserving.CampaignImpressionList}
 */
public  final class CampaignImpressionList implements JSONSerializable, JSONDeserializable {
  private List<CampaignImpression> alreadySeenCampaigns_ = new ArrayList<>();

  public CampaignImpressionList() {
  }

  public CampaignImpressionList(CampaignImpressionList toCopy) {
    this.alreadySeenCampaigns_.addAll(toCopy.alreadySeenCampaigns_);
  }

  @Override
  public void fromJSON(JSONObject json) {
    if (json == null) return;
    alreadySeenCampaigns_ = new ArrayList<>();
    JSONArray alreadySeenCampaigns = json.optJSONArray("alreadySeenCampaigns");
    for (int i = 0; i < alreadySeenCampaigns.length(); i++) {
      JSONObject campaignJson = alreadySeenCampaigns.optJSONObject(i);
      if (campaignJson == null) continue;
      CampaignImpression impression = new CampaignImpression();
      impression.fromJSON(campaignJson);
      alreadySeenCampaigns_.add(impression);
    }
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject result = new JSONObject();
    JSONArray alreadySeenCampaigns = new JSONArray();
    result.put("alreadySeenCampaigns", alreadySeenCampaigns);
    for (CampaignImpression impression : alreadySeenCampaigns_) {
      alreadySeenCampaigns.put(impression.toJSON());
    }
    return result;
  }

  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  public java.util.List<CampaignImpression> getAlreadySeenCampaignsList() {
    return alreadySeenCampaigns_;
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  public java.util.List<? extends CampaignImpression>
      getAlreadySeenCampaignsOrBuilderList() {
    return alreadySeenCampaigns_;
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  public int getAlreadySeenCampaignsCount() {
    return alreadySeenCampaigns_.size();
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  public CampaignImpression getAlreadySeenCampaigns(int index) {
    return alreadySeenCampaigns_.get(index);
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  public CampaignImpression getAlreadySeenCampaignsOrBuilder(
      int index) {
    return alreadySeenCampaigns_.get(index);
  }

  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  private void setAlreadySeenCampaigns(
      int index, CampaignImpression value) {
    if (value == null) {
      throw new NullPointerException();
    }
    alreadySeenCampaigns_.set(index, value);
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  public void addAlreadySeenCampaigns(CampaignImpression value) {
    if (value == null) {
      throw new NullPointerException();
    }
    alreadySeenCampaigns_.add(value);
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  private void addAlreadySeenCampaigns(
      int index, CampaignImpression value) {
    if (value == null) {
      throw new NullPointerException();
    }
    alreadySeenCampaigns_.add(index, value);
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  private void addAllAlreadySeenCampaigns(
      Collection<? extends CampaignImpression> values) {

    alreadySeenCampaigns_.addAll(values);
  }
  /**
   * <pre>
   * a list of campaigns that have already been rendered by the client so that
   * service can filter them out while doing search of applicable messages
   * </pre>
   *
   * <code>repeated .inappmessaging.v1.sdkserving.CampaignImpression already_seen_campaigns = 1;</code>
   */
  private void removeAlreadySeenCampaigns(int index) {
    alreadySeenCampaigns_.remove(index);
  }

}


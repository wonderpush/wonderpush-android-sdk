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

package com.wonderpush.sdk;

/** Provides the following about any message, */
public class NotificationMetadata {
  private final String campaignId;
  private final String notificationId;
  private final String viewId;
  private final boolean isTestMessage;

  public NotificationMetadata(NotificationModel notif) {
    this(notif.getCampaignId(), notif.getNotificationId(), notif.getViewId(), false);
  }

  /**
   * This is only used by the IAM internal SDK
   *
   * @hide
   */
  public NotificationMetadata(String campaignId, String notificationId, String viewId, boolean isTestMessage) {
    this.campaignId = campaignId;
    this.notificationId = notificationId;
    this.viewId = viewId;
    this.isTestMessage = isTestMessage;
  }

  public String getCampaignId() {
    return campaignId;
  }

  public String getNotificationId() {
    return notificationId;
  }

  public String getViewId() {
    return viewId;
  }

  public boolean getIsTestMessage() {
    return isTestMessage;
  }
}

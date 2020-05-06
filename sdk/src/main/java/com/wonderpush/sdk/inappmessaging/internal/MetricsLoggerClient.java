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

package com.wonderpush.sdk.inappmessaging.internal;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.NotificationMetadata;
import com.wonderpush.sdk.TimeSync;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks.InAppMessagingDismissType;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks.InAppMessagingErrorReason;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import javax.inject.Inject;

/**
 * Class to log actions to engagementMetrics
 *
 * @hide
 */
public class MetricsLoggerClient {

  private final DeveloperListenerManager developerListenerManager;
  private final WonderPush.InternalEventTracker internalEventTracker;

  @Inject
  public MetricsLoggerClient(DeveloperListenerManager developerListenerManager,
                             WonderPush.InternalEventTracker internalEventTracker) {
    this.developerListenerManager = developerListenerManager;
    this.internalEventTracker = internalEventTracker;
  }

  private void logInternalEvent(String eventName, NotificationMetadata metadata) {
    JSONObject eventData = new JSONObject();
    try {
      if (metadata.getCampaignId() != null) eventData.put("campaignId", metadata.getCampaignId());
      if (metadata.getNotificationId() != null) eventData.put("notificationId", metadata.getNotificationId());
      if (metadata.getViewId() != null) eventData.put("viewId", metadata.getViewId());
      eventData.put("actionDate", TimeSync.getTime());
      this.internalEventTracker.trackInternalEvent(eventName, eventData);
    } catch (JSONException e) {}
  }

  /** Log impression */
  public void logImpression(InAppMessage message) {
    this.logInternalEvent("@INAPP_VIEWED", message.getNotificationMetadata());

    // No matter what, always trigger developer callbacks
    developerListenerManager.impressionDetected(message);
  }

  /** Log click */
  public void logMessageClick(InAppMessage message, List<ActionModel> actions) {
    this.logInternalEvent("@INAPP_CLICKED", message.getNotificationMetadata());

    // No matter what, always trigger developer callbacks
    developerListenerManager.messageClicked(message, actions);
  }

  /** Log Rendering error */
  public void logRenderError(InAppMessage message, InAppMessagingErrorReason errorReason) {
    // TODO: log render error
    // No matter what, always trigger developer callbacks
    developerListenerManager.displayErrorEncountered(message, errorReason);
  }

  /** Log dismiss */
  public void logDismiss(InAppMessage message, InAppMessagingDismissType dismissType) {
    // TODO: log render dismiss
  }

}

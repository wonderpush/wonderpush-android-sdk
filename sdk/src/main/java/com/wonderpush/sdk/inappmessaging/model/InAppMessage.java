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

import android.app.Notification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.NotificationMetadata;

import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

/** Encapsulates an In App Message. */
public abstract class InAppMessage {
  public enum ButtonType {
    UNDEFINED, PRIMARY, SECONDARY,
  }
  public enum CloseButtonPosition {
    OUTSIDE, INSIDE, NONE,
  }
  public interface InAppMessageWithImage {
    @Nullable
    ImageData getImageData();
  }

  MessageType messageType;
  NotificationMetadata notificationMetadata;
  private JSONObject data;

  /** @hide */
  public InAppMessage(
          NotificationMetadata notificationMetadata, MessageType messageType, JSONObject data) {
    this.notificationMetadata = notificationMetadata;
    this.messageType = messageType;
    this.data = data;
  }

  /** Gets the {@link MessageType} of the message */
  @Nullable
  public MessageType getMessageType() {
    return messageType;
  }
  /** Gets the {@link NotificationMetadata} of the message */
  @Nullable
  public NotificationMetadata getNotificationMetadata() {
    return notificationMetadata;
  }

  @NonNull
  public JSONObject getData() {
    return data;
  }


  abstract public ButtonType getButtonType(List<ActionModel> actions);

  protected boolean actionsEqual(@NonNull List<ActionModel> actions1, @NonNull List<ActionModel> actions2) {
    if (actions1.size() != actions2.size()) return false;
    for (int i = 0; i < actions1.size(); i++) {
      if (actions1.get(i) != actions2.get(i)) return false;
    }
    return true;
  }
}

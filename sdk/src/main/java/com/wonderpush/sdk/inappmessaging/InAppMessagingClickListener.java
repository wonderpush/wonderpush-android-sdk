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

package com.wonderpush.sdk.inappmessaging;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;

import java.util.List;

/**
 * An interface you can implement and feed to {@link InAppMessaging#addClickListener(InAppMessagingClickListener)} in order to get called back when users click on in-app messages.
 */
public interface InAppMessagingClickListener {

  /**
   * Called when a message is tapped (ie: button, in the modal view) and which actions were triggered.
   * @param inAppMessage
   * @param actions
   */
  void messageClicked(@NonNull InAppMessage inAppMessage, @NonNull List<ActionModel> actions);

  /**
   * Called when `trackClick` was called on the display callback, along with the buttonLabel that triggered it.
   * @param inAppMessage
   * @param buttonLabel
   */
  void clickTracked(@NonNull InAppMessage inAppMessage, @Nullable String buttonLabel);
}

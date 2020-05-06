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

import android.support.annotation.NonNull;

import com.wonderpush.sdk.inappmessaging.model.InAppMessage;

/**
 * An interface you can implement in order to display messages when an error occurs during the presentation of an in-app message.
 * Pass an instance to {@link InAppMessaging#addDisplayErrorListener(InAppMessagingDisplayErrorListener)}
 */
public interface InAppMessagingDisplayErrorListener {

  /**
   * The implementation should optionally display an error message to the user.
   * @param inAppMessage
   * @param errorReason
   */
  void displayErrorEncountered(
          @NonNull InAppMessage inAppMessage,
          @NonNull InAppMessagingDisplayCallbacks.InAppMessagingErrorReason errorReason);
}

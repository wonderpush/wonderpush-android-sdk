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

import com.wonderpush.sdk.ActionModel;

import java.util.List;

/**
 * An interface implemented by the in-app messaging SDK that gets passed to the {@link InAppMessagingDisplay} instance
 * to allow reporting of clicks and views.
 */
public interface InAppMessagingDisplayCallbacks {

  /**
   * Log the impression
   */
  void impressionDetected();

  /**
   * Log the dismiss
   * @param dismissType
   */
  void messageDismissed(@NonNull InAppMessagingDismissType dismissType);

  /**
   * Log the click, passing along the corresponding actions.
   * @param actions
   */
  void messageClicked(@NonNull List<ActionModel> actions);

  /**
   * Report display errors.
   * @param inAppMessagingErrorReason
   */
  void displayErrorEncountered(@NonNull InAppMessagingErrorReason inAppMessagingErrorReason);

  /**
   * Describes how the user dismissed an in-app message.
   */
  enum InAppMessagingDismissType {
    // Unspecified dismiss type
    UNKNOWN_DISMISS_TYPE,

    // Message was dismissed automatically after a timeout
    AUTO,

    // Message was dismissed by clicking on cancel button or outside the message
    CLICK,

    // Message was swiped
    SWIPE
  }

  /**
   * Describes the common errors encountered when displaying in-app messages.
   */
  enum InAppMessagingErrorReason {
    // Generic error
    UNSPECIFIED_RENDER_ERROR,

    // Failure to fetch the image
    IMAGE_FETCH_ERROR,

    // Failure to display the image
    IMAGE_DISPLAY_ERROR,

    // Image has an unsupported format
    IMAGE_UNSUPPORTED_FORMAT
  }
}

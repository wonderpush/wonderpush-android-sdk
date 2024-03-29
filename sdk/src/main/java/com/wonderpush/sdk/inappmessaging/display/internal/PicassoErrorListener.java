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

package com.wonderpush.sdk.inappmessaging.display.internal;

// Picasso 's api forces us to listen to errors only using a global listener set on the picasso
// singleton. Since we initialize picasso from a static context and the in app message param to the
// logError method is not available statically, we are forced to introduce a error listener with
// mutable state so that the error from picasso can be translated to a logError on
// iam headless, with the in app message as a parameter

import android.net.Uri;

import com.squareup.picasso3.Picasso;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks.InAppMessagingErrorReason;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;

import java.io.IOException;

import javax.inject.Inject;

/** @hide */
@InAppMessagingScope
public class PicassoErrorListener implements Picasso.Listener {
  private InAppMessage inAppMessage;
  private InAppMessagingDisplayCallbacks displayCallbacks;

  @Inject
  PicassoErrorListener() {}

  public void setInAppMessage(
      InAppMessage inAppMessage, InAppMessagingDisplayCallbacks displayCallbacks) {
    this.inAppMessage = inAppMessage;
    this.displayCallbacks = displayCallbacks;
  }

  @Override
  public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
    if (inAppMessage != null && displayCallbacks != null) {
      if (exception instanceof IOException
          && exception.getLocalizedMessage().contains("Failed to decode")) {
        displayCallbacks.displayErrorEncountered(
            InAppMessagingErrorReason.IMAGE_UNSUPPORTED_FORMAT);
      } else {
        displayCallbacks.displayErrorEncountered(
            InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR);
      }
    }
  }
}

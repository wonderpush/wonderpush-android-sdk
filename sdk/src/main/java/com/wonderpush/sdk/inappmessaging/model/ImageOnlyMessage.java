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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.NotificationMetadata;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/** Encapsulates an In App ImageOnly Message. */
public class ImageOnlyMessage extends InAppMessage implements InAppMessage.InAppMessageWithImage {
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  @NonNull private ImageData imageData;

  @NonNull private List<ActionModel> actions;

  /** @hide */
  @Override
  public int hashCode() {
    int actionHash = actions != null ? actions.hashCode() : 0;
    return imageData.hashCode() + actionHash;
  }

  /** @hide */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true; // same instance
    }
    if (!(o instanceof ImageOnlyMessage)) {
      return false; // not the correct instance type
    }
    ImageOnlyMessage i = (ImageOnlyMessage) o;
    if (hashCode() != i.hashCode()) {
      return false; // the hashcodes don't match
    }
    if ((actions == null && i.actions != null) || (actions != null && !actions.equals(i.actions))) {
      return false; // the actions don't match
    }
    if (imageData.equals(i.imageData)) {
      return true; // everything matches
    }
    return false;
  }
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  private ImageOnlyMessage(
      @NonNull NotificationMetadata notificationMetadata,
      @NonNull ImageData imageData,
      @NonNull List<ActionModel> actions,
      @NonNull JSONObject data) {
    super(notificationMetadata, MessageType.IMAGE_ONLY, data);
    this.imageData = imageData;
    this.actions = actions;
  }

  /** Gets the {@link ImageData} associated with this message */
  @NonNull
  public ImageData getImageData() {
    return imageData;
  }

  /** Gets the {@link ActionModel}s associated with this message */
  @NonNull
  public List<ActionModel> getActions() {
    return actions;
  }

  /**
   * only used by headless sdk and tests
   *
   * @hide
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link ImageOnlyMessage}
   *
   * @hide
   */
  public static class Builder {
    @Nullable ImageData imageData;
    @Nullable
    List<ActionModel> actions;

    public Builder setImageData(@Nullable ImageData imageData) {
      this.imageData = imageData;
      return this;
    }

    public Builder setActions(@Nullable List<ActionModel> actions) {
      this.actions = actions;
      return this;
    }

    public ImageOnlyMessage build(
            NotificationMetadata notificationMetadata, @NonNull JSONObject data) {
      if (imageData == null) {
        throw new IllegalArgumentException("ImageOnly model must have image data");
      }
      return new ImageOnlyMessage(notificationMetadata, imageData, actions == null ? Collections.emptyList() : actions, data);
    }
  }
}

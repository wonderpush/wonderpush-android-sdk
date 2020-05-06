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

import android.support.annotation.Nullable;

import com.wonderpush.sdk.JSONUtil;

import org.json.JSONObject;

/** Encapsulates any button used in an In App Message. */
public class Button {
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  private final Text text;

  private final String buttonHexColor;

  /** @hide */
  @Override
  public int hashCode() {
    return (text == null ? 0 : text.hashCode())
            + (buttonHexColor == null ? 0 : buttonHexColor.hashCode());
  }

  /** @hide */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true; // same instance
    }
    if (!(o instanceof Button)) {
      return false; // not the correct instance type
    }
    Button b = (Button) o;
    if (hashCode() != b.hashCode()) {
      return false; // the hashcodes don't match
    }
    if ((text == null && b.text != null) || (text != null && !text.equals(b.text))) {
      return false; // the texts don't match
    }
    if ((buttonHexColor == null && b.buttonHexColor != null) || (buttonHexColor != null && !buttonHexColor.equals(b.buttonHexColor))) {
      return false; // button colors don't match
    }
    return true;
  }

  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  public Button(Text text, String buttonHexColor) {
    this.text = text;
    this.buttonHexColor = buttonHexColor;
  }

  /** Gets the {@link Text} associated with this button */
  public Text getText() {
    return text;
  }

  /** Gets the background hex color associated with this button */
  public String getButtonHexColor() {
    return buttonHexColor;
  }

  /**
   * @hide
   */
  public static Button.Builder builder() {
    return new Button.Builder();
  }

  /**
   * Builder for {@link Button}
   *
   * @hide
   */
  public static class Builder {
    @Nullable private Text text;
    @Nullable private String buttonHexColor;

    public Button.Builder fromJSON(JSONObject data) {
      if (data != null) {
        if (data.optJSONObject("text") != null) {
          this.setText(Text.builder().fromJSON(data.optJSONObject("text")).build());
        }
        this.setButtonHexColor(JSONUtil.getString(data, "buttonHexColor"));
      }
      return this;
    }

    public Button.Builder setText(@Nullable Text text) {
      this.text = text;
      return this;
    }

    public Button.Builder setButtonHexColor(@Nullable String buttonHexColor) {
      this.buttonHexColor = buttonHexColor;
      return this;
    }

    public Button build() {
      return new Button(text, buttonHexColor);
    }
  }

}

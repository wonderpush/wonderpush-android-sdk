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

import androidx.annotation.Nullable;

import com.wonderpush.sdk.JSONUtil;

import org.json.JSONObject;

/** Encapsulates any text used in an In App Message. */
public class Text {
  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  private final String text;

  private final String hexColor;

  /** @hide */
  @Override
  public int hashCode() {
    return (text == null ? 0 : text.hashCode())
            + (hexColor == null ? 0 : hexColor.hashCode());
  }

  /** @hide */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true; // same instance
    }
    if (!(o instanceof Text)) {
      return false; // not the correct instance type
    }
    Text t = (Text) o;
    if (hashCode() != t.hashCode()) {
      return false; // the hashcodes don't match
    }
    if ((text == null && t.text != null) || (text != null && !text.equals(t.text))) {
      return false; // the texts don't match
    }
    if ((hexColor == null && t.hexColor != null) || (hexColor != null && !hexColor.equals(t.hexColor))) {
      return false; // the hex codes don't match
    }
    return true;
  }

  /*
   * !!!!!WARNING!!!!! We are overriding equality in this class. Please add equality checks for all
   * new private class members.
   */
  public Text(String text, String hexColor) {
    this.text = text;
    this.hexColor = hexColor;
  }

  public static Text fromJSON(JSONObject data) {
    if (data == null) return null;
    String text = data.optString("text");
    String hexColor = data.optString("hexColor");
    if (text == null) return null;
    return new Text(text, hexColor);
  }

  /** Gets the text */
  public String getText() {
    return text;
  }

  /** Gets the hex color of this text */
  public String getHexColor() {
    return hexColor;
  }

  /**
   * @hide
   */
  public static Text.Builder builder() {
    return new Text.Builder();
  }

  /**
   * Builder for {@link Text}
   *
   * @hide
   */
  public static class Builder {
    @Nullable private String text;
    @Nullable private String hexColor;

    public Text.Builder fromJSON(JSONObject data) {
      if (data != null) {
        this.setText(JSONUtil.getString(data, "text"));
        this.setHexColor(JSONUtil.getString(data, "hexColor"));
      }
      return this;
    }

    public Text.Builder setText(@Nullable String text) {
      this.text = text;
      return this;
    }

    public Text.Builder setHexColor(@Nullable String hexColor) {
      this.hexColor = hexColor;
      return this;
    }

    public Text build() {
      return new Text(text, hexColor);
    }
  }

}

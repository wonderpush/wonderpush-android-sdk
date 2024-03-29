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

import androidx.annotation.Keep;

/** Template type of an in-app message */
@Keep
public enum MessageType {
  @Keep
  UNSUPPORTED,
  @Keep
  MODAL,
  @Keep
  IMAGE_ONLY,
  @Keep
  BANNER,
  @Keep
  CARD,
  @Keep
  WEBVIEW
}

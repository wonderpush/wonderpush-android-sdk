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

/**
 * Value object class representing rate limits.
 *
 * @hide
 */
public class RateLimit {
  private String _limiterKey;
  private long _limit;
  private long _timeToLiveMillis;
  public RateLimit(String limiterKey, long limit, long timeToLiveMillis) {
    _limiterKey = limiterKey;
    _limit = limit;
    _timeToLiveMillis = timeToLiveMillis;
  }
  public String limiterKey() { return _limiterKey; }

  public long limit() { return _limit; }

  public long timeToLiveMillis() { return _timeToLiveMillis; };

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof RateLimit)) return false;
    RateLimit other = (RateLimit) obj;
    if (other.limiterKey() == null) {
      if (limiterKey() != null) return false;
    } else if (!other.limiterKey().equals(limiterKey())) return false;
    if (limiterKey() == null) {
      if (other.limiterKey() != null) return false;
    } else if (!limiterKey().equals(other.limiterKey())) return false;
    return limit() == other.limit() && timeToLiveMillis() == other.timeToLiveMillis();
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @NonNull
  @Override
  public String toString() {
    return String.format("RateLimit{limiterKey=%s, limit=%d, timeToLiveMillis=%d}", limiterKey() != null ? limiterKey() : "", limit(), timeToLiveMillis());
  }
}

// Generated by the protocol buffer compiler.  DO NOT EDIT!

package com.wonderpush.sdk.inappmessaging.internal;

import com.wonderpush.sdk.JSONDeserializable;
import com.wonderpush.sdk.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class RateLimitProto {
  private RateLimitProto() {}

  /**
   * <pre>
   * Actions performed on the in app message
   * </pre>
   *
   * Protobuf type {@code com.wonderpush.sdk.inappmessaging.internal.RateLimit}
   */
  public  static final class RateLimit implements JSONSerializable, JSONDeserializable {
    private Map<String, Counter> limits_ = new HashMap<>();

    public RateLimit() {
    }

    public RateLimit(RateLimit toCopy) {
      this();
      setLimits(toCopy.getLimitsMap());
    }

    @Override
    public JSONObject toJSON() throws JSONException {
      JSONObject rtn = new JSONObject();
      JSONObject limits = new JSONObject();
      for (Map.Entry<String, Counter> entry : internalGetLimits().entrySet()) {
          limits.put(entry.getKey(), entry.getValue().toJSON());
      }
      rtn.put("limits", limits);
      return rtn;
    }

    @Override
    public void fromJSON(JSONObject json) {
      JSONObject limitsJson = json.optJSONObject("limits");
      if (limitsJson != null) {
        Iterator<String> keys = limitsJson.keys();
        while (keys.hasNext()) {
          String key = keys.next();
          JSONObject counterJson = limitsJson.optJSONObject(key);
          if (counterJson != null) {
            Counter counter = new Counter();
            counter.fromJSON(counterJson);
            this.putLimit(key, counter);
          }
        }
      }
    }

    private Map<String, Counter>
    internalGetLimits() {
      return limits_;
    }

    public void setLimits(Map<String, Counter> limits) {
      Map<String, Counter> map = internalGetLimits();
      map.clear();
      map.putAll(limits);
    }

    public void putLimit(String key, Counter counter) {
      Map<String, Counter> map = internalGetLimits();
      map.put(key, counter);
    }

    public int getLimitsCount() {
      return internalGetLimits().size();
    }
    /**
     * <pre>
     * map from limiter key to counter
     * </pre>
     *
     * <code>map&lt;string, .com.wonderpush.sdk.inappmessaging.internal.Counter&gt; limits = 1;</code>
     */

    public boolean containsLimits(
        String key) {
      if (key == null) { throw new NullPointerException(); }
      return internalGetLimits().containsKey(key);
    }
    /**
     * Use {@link #getLimitsMap()} instead.
     */
    @Deprecated
    public java.util.Map<String, Counter> getLimits() {
      return getLimitsMap();
    }
    /**
     * <pre>
     * map from limiter key to counter
     * </pre>
     *
     * <code>map&lt;string, .com.wonderpush.sdk.inappmessaging.internal.Counter&gt; limits = 1;</code>
     */

    public java.util.Map<String, Counter> getLimitsMap() {
      return java.util.Collections.unmodifiableMap(
          internalGetLimits());
    }
    /**
     * <pre>
     * map from limiter key to counter
     * </pre>
     *
     * <code>map&lt;string, .com.wonderpush.sdk.inappmessaging.internal.Counter&gt; limits = 1;</code>
     */

    public Counter getLimitsOrDefault(
        String key,
        Counter defaultValue) {
      if (key == null) { throw new NullPointerException(); }
      java.util.Map<String, Counter> map =
          internalGetLimits();
      return map.containsKey(key) ? map.get(key) : defaultValue;
    }
    /**
     * <pre>
     * map from limiter key to counter
     * </pre>
     *
     * <code>map&lt;string, .com.wonderpush.sdk.inappmessaging.internal.Counter&gt; limits = 1;</code>
     */

    public Counter getLimitsOrThrow(
        String key) {
      if (key == null) { throw new NullPointerException(); }
      java.util.Map<String, Counter> map =
          internalGetLimits();
      if (!map.containsKey(key)) {
        throw new IllegalArgumentException();
      }
      return map.get(key);
    }

  }

  /**
   * Protobuf type {@code com.wonderpush.sdk.inappmessaging.internal.Counter}
   */
  public  static final class Counter implements JSONSerializable, JSONDeserializable {
    private long value_;
    private long startTimeEpoch_;

    public Counter() {
    }

    public Counter(Counter toCopy) {
      this.value_ = toCopy.value_;
      this.startTimeEpoch_ = toCopy.startTimeEpoch_;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
      JSONObject rtn = new JSONObject();
      rtn.put("value", getValue());
      rtn.put("startTimeEpoch", getStartTimeEpoch());
      return rtn;
    }

    @Override
    public void fromJSON(JSONObject json) {
      Object valueJson = json.opt("value");
      if (valueJson instanceof Number) {
        setValue(((Number) valueJson).longValue());
      }
      Object startTimeEpochJson = json.opt("startTimeEpoch");
      if (startTimeEpochJson instanceof Number) {
        setValue(((Number) startTimeEpochJson).longValue());
      }
    }

    /**
     * <pre>
     * value of the counter
     * </pre>
     *
     * <code>optional int64 value = 1;</code>
     */
    public long getValue() {
      return value_;
    }
    /**
     * <pre>
     * value of the counter
     * </pre>
     *
     * <code>optional int64 value = 1;</code>
     */
    public void setValue(long value) {
      value_ = value;
    }
    /**
     * <pre>
     * time at which the counter was initiated
     * </pre>
     *
     * <code>optional int64 start_time_epoch = 2;</code>
     */
    public long getStartTimeEpoch() {
      return startTimeEpoch_;
    }
    /**
     * <pre>
     * time at which the counter was initiated
     * </pre>
     *
     * <code>optional int64 start_time_epoch = 2;</code>
     */
    public void setStartTimeEpoch(long value) {
      startTimeEpoch_ = value;
    }

  }

}

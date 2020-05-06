package com.wonderpush.sdk.inappmessaging.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CommonTypesProto {
  private CommonTypesProto() {}
  /**
   * <pre>
   * User-specified events that can activate a campaign for an app instance.
   * Note, this just controls eligibility. Client-side evaluation of rate limits
   * will determine whether or not the message is actually displayed in the app.
   * If the app is already in the foreground when a campaign is started, the
   * in-app message will not be displayed until the app is re-opened.
   * </pre>
   *
   * Protobuf enum {@code .inappmessaging.Trigger}
   */
  public enum Trigger {
    /**
     * <code>UNKNOWN_TRIGGER = 0;</code>
     */
    UNKNOWN_TRIGGER(0),
    /**
     * <pre>
     * App is launched.
     * </pre>
     *
     * <code>APP_LAUNCH = 1;</code>
     */
    APP_LAUNCH(1),
    /**
     * <pre>
     * App has already launched and is brought into the foreground.
     * </pre>
     *
     * <code>ON_FOREGROUND = 2;</code>
     */
    ON_FOREGROUND(2),
    UNRECOGNIZED(-1),
    ;

    /**
     * <code>UNKNOWN_TRIGGER = 0;</code>
     */
    public static final int UNKNOWN_TRIGGER_VALUE = 0;
    /**
     * <pre>
     * App is launched.
     * </pre>
     *
     * <code>APP_LAUNCH = 1;</code>
     */
    public static final int APP_LAUNCH_VALUE = 1;
    /**
     * <pre>
     * App has already launched and is brought into the foreground.
     * </pre>
     *
     * <code>ON_FOREGROUND = 2;</code>
     */
    public static final int ON_FOREGROUND_VALUE = 2;


    public final int getNumber() {
      return value;
    }

    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @Deprecated
    public static Trigger valueOf(int value) {
      return forNumber(value);
    }

    public static Trigger forNumber(int value) {
      switch (value) {
        case 0: return UNKNOWN_TRIGGER;
        case 1: return APP_LAUNCH;
        case 2: return ON_FOREGROUND;
        default: return null;
      }
    }

    private final int value;

    private Trigger(int value) {
      this.value = value;
    }
  }

  /**
   * <pre>
   * Triggering condition for determing when the campaign will be shown to
   * targeted users. Can be either a IamTrigger, or a Scion Event
   * </pre>
   *
   * Protobuf type {@code inappmessaging.TriggeringCondition}
   */
  public  static final class TriggeringCondition {
    public static TriggeringCondition fromJSON(JSONObject triggerJson) {
      if (null == triggerJson) return null;
      TriggeringCondition result = new TriggeringCondition();
      String systemEvent = triggerJson.optString("systemEvent");
      result.setDelay(triggerJson.optLong("delay", 0));
      JSONObject eventJson = triggerJson.optJSONObject("event");
      if (systemEvent != null) {
        if (systemEvent.equals("ON_FOREGROUND")) {
          result.setIamTrigger(Trigger.ON_FOREGROUND);
        } else if (systemEvent.equals("APP_LAUNCH")) {
          // APP_LAUNCH and ON_FOREGROUND are equivalent on Android
          result.setIamTrigger(Trigger.ON_FOREGROUND);
        } else {
          result.setIamTrigger(Trigger.UNRECOGNIZED);
        }
      }
      if (eventJson != null) {
        result.setEvent(Event.fromJSON(eventJson));
      }
      return result;
    }

    private long delay;

    private void setDelay(long delay) {
      this.delay = delay;
    }

    public long getDelay() {
      return delay;
    }

    private TriggeringCondition() {
    }
    private int conditionCase_ = 0;
    private Object condition_;
    public enum ConditionCase {
      IAM_TRIGGER(1),
      EVENT(2),
      CONDITION_NOT_SET(0);
      private final int value;
      private ConditionCase(int value) {
        this.value = value;
      }
      /**
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @Deprecated
      public static ConditionCase valueOf(int value) {
        return forNumber(value);
      }

      public static ConditionCase forNumber(int value) {
        switch (value) {
          case 1: return IAM_TRIGGER;
          case 2: return EVENT;
          case 0: return CONDITION_NOT_SET;
          default: return null;
        }
      }
      public int getNumber() {
        return this.value;
      }
    };

    public ConditionCase
    getConditionCase() {
      return ConditionCase.forNumber(
          conditionCase_);
    }

    private void clearCondition() {
      conditionCase_ = 0;
      condition_ = null;
    }

    /**
     * <code>optional .inappmessaging.Trigger iam_trigger = 1;</code>
     */
    public int getIamTriggerValue() {
      if (conditionCase_ == 1) {
        return (Integer) condition_;
      }
      return 0;
    }
    /**
     * <code>optional .inappmessaging.Trigger iam_trigger = 1;</code>
     */
    public CommonTypesProto.Trigger getIamTrigger() {
      if (conditionCase_ == 1) {
        CommonTypesProto.Trigger result = CommonTypesProto.Trigger.forNumber((Integer) condition_);
        return result == null ? CommonTypesProto.Trigger.UNRECOGNIZED : result;
      }
      return CommonTypesProto.Trigger.UNKNOWN_TRIGGER;
    }
    /**
     * <code>optional .inappmessaging.Trigger iam_trigger = 1;</code>
     */
    private void setIamTriggerValue(int value) {
      conditionCase_ = 1;
      condition_ = value;
    }
    /**
     * <code>optional .inappmessaging.Trigger iam_trigger = 1;</code>
     */
    private void setIamTrigger(CommonTypesProto.Trigger value) {
      if (value == null) {
        throw new NullPointerException();
      }
      conditionCase_ = 1;
      condition_ = value.getNumber();
    }
    /**
     * <code>optional .inappmessaging.Trigger iam_trigger = 1;</code>
     */
    private void clearIamTrigger() {
      if (conditionCase_ == 1) {
        conditionCase_ = 0;
        condition_ = null;
      }
    }

    /**
     * <code>optional .inappmessaging.Event event = 2;</code>
     */
    public CommonTypesProto.Event getEvent() {
      if (conditionCase_ == 2) {
         return (CommonTypesProto.Event) condition_;
      }
      return null;
    }
    /**
     * <code>optional .inappmessaging.Event event = 2;</code>
     */
    private void setEvent(CommonTypesProto.Event value) {
      if (value == null) {
        throw new NullPointerException();
      }
      condition_ = value;
      conditionCase_ = 2;
    }

  }

  /**
   * Protobuf type {@code inappmessaging.Event}
   */
  public  static final class Event {
    private List<TriggerParam> triggerParams_ = new ArrayList<>();
    private long timestampMillis_;
    private long previousTimestampMillis_;
    private int count_;
    private String name_;

    private Event() {
    }

    public static Event fromJSON(JSONObject eventJson) {
      if (null == eventJson) return null;
      String type = eventJson.optString("type");
      Event result = new Event();
      result.setName(type);
      return result;
    }

    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    public java.util.List<TriggerParam> getTriggerParamsList() {
      return triggerParams_;
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    public java.util.List<? extends TriggerParam>
        getTriggerParamsOrBuilderList() {
      return triggerParams_;
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    public int getTriggerParamsCount() {
      return triggerParams_.size();
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    public CommonTypesProto.TriggerParam getTriggerParams(int index) {
      return triggerParams_.get(index);
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    public CommonTypesProto.TriggerParam getTriggerParamsOrBuilder(
        int index) {
      return triggerParams_.get(index);
    }

    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    private void setTriggerParams(
        int index, CommonTypesProto.TriggerParam value) {
      if (value == null) {
        throw new NullPointerException();
      }
      triggerParams_.set(index, value);
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    private void addTriggerParams(CommonTypesProto.TriggerParam value) {
      if (value == null) {
        throw new NullPointerException();
      }
      triggerParams_.add(value);
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    private void addTriggerParams(
        int index, CommonTypesProto.TriggerParam value) {
      if (value == null) {
        throw new NullPointerException();
      }
      triggerParams_.add(index, value);
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    private void addAllTriggerParams(
        Collection<? extends TriggerParam> values) {
      triggerParams_.addAll(values);
    }
    /**
     * <pre>
     * Triggers that represent the context for the in app message
     * </pre>
     *
     * <code>repeated .inappmessaging.TriggerParam trigger_params = 1;</code>
     */
    private void removeTriggerParams(int index) {
      triggerParams_.remove(index);
    }

    /**
     * <pre>
     * The event name. Event name length will be limited to something short like
     * 24 or 32 chars. The exact max length limit is TBD.
     * </pre>
     *
     * <code>optional string name = 2;</code>
     */
    public String getName() {
      return name_;
    }
    /**
     * <pre>
     * The event name. Event name length will be limited to something short like
     * 24 or 32 chars. The exact max length limit is TBD.
     * </pre>
     *
     * <code>optional string name = 2;</code>
     */
    private void setName(
        String value) {
      name_ = value;
    }

    /**
     * <pre>
     * UTC client time when the transaction happened in millis.
     * </pre>
     *
     * <code>optional int64 timestamp_millis = 3;</code>
     */
    public long getTimestampMillis() {
      return timestampMillis_;
    }
    /**
     * <pre>
     * UTC client time when the transaction happened in millis.
     * </pre>
     *
     * <code>optional int64 timestamp_millis = 3;</code>
     */
    private void setTimestampMillis(long value) {

      timestampMillis_ = value;
    }

    /**
     * <pre>
     * UTC client time when the transaction happened in millis.
     * </pre>
     *
     * <code>optional int64 previous_timestamp_millis = 4;</code>
     */
    public long getPreviousTimestampMillis() {
      return previousTimestampMillis_;
    }
    /**
     * <pre>
     * UTC client time when the transaction happened in millis.
     * </pre>
     *
     * <code>optional int64 previous_timestamp_millis = 4;</code>
     */
    private void setPreviousTimestampMillis(long value) {

      previousTimestampMillis_ = value;
    }

    /**
     * <pre>
     * Events without timestamps will be grouped together by set of params and
     * will be reported with occurrence count. This is lossless client side
     * aggregation to pack the data in more compact form. Events with different
     * set of params will be logged with different EventParam message.
     * </pre>
     *
     * <code>optional int32 count = 5;</code>
     */
    public int getCount() {
      return count_;
    }
    /**
     * <pre>
     * Events without timestamps will be grouped together by set of params and
     * will be reported with occurrence count. This is lossless client side
     * aggregation to pack the data in more compact form. Events with different
     * set of params will be logged with different EventParam message.
     * </pre>
     *
     * <code>optional int32 count = 5;</code>
     */
    private void setCount(int value) {

      count_ = value;
    }

  }

  /**
   * Protobuf type {@code inappmessaging.TriggerParam}
   */
  public  static final class TriggerParam {
    private String name_;
    private String stringValue_;
    private long intValue_;
    private float floatValue_;
    private double doubleValue_;

    private TriggerParam() {
    }

    /**
     * <pre>
     * The name of the trigger param. The max size of the event name is TBD but it
     * will be constrained to 24 or 32 chars.
     * </pre>
     *
     * <code>optional string name = 1;</code>
     */
    public String getName() {
      return name_;
    }
    /**
     * <pre>
     * The name of the trigger param. The max size of the event name is TBD but it
     * will be constrained to 24 or 32 chars.
     * </pre>
     *
     * <code>optional string name = 1;</code>
     */
    private void setName(
        String value) {
      if (value == null) {
    throw new NullPointerException();
  }

      name_ = value;
    }

    /**
     * <code>optional string string_value = 2;</code>
     */
    public String getStringValue() {
      return stringValue_;
    }
    /**
     * <code>optional string string_value = 2;</code>
     */
    private void setStringValue(
        String value) {
      if (value == null) {
    throw new NullPointerException();
  }

      stringValue_ = value;
    }

    /**
     * <code>optional int64 int_value = 3;</code>
     */
    public long getIntValue() {
      return intValue_;
    }
    /**
     * <code>optional int64 int_value = 3;</code>
     */
    private void setIntValue(long value) {

      intValue_ = value;
    }
    /**
     * <code>optional int64 int_value = 3;</code>
     */
    private void clearIntValue() {

      intValue_ = 0L;
    }

    /**
     * <code>optional float float_value = 4;</code>
     */
    public float getFloatValue() {
      return floatValue_;
    }
    /**
     * <code>optional float float_value = 4;</code>
     */
    private void setFloatValue(float value) {

      floatValue_ = value;
    }
    /**
     * <code>optional float float_value = 4;</code>
     */
    private void clearFloatValue() {

      floatValue_ = 0F;
    }

    /**
     * <code>optional double double_value = 5;</code>
     */
    public double getDoubleValue() {
      return doubleValue_;
    }
    /**
     * <code>optional double double_value = 5;</code>
     */
    private void setDoubleValue(double value) {

      doubleValue_ = value;
    }

  }

  /**
   * <pre>
   * Priority of the campaign.
   * Used to select the most important messages amongst a set of eligible ones.
   * </pre>
   *
   * Protobuf type {@code inappmessaging.Priority}
   */
  public  static final class Priority {
    private int value_;

    public Priority() {
    }

    /**
     * <pre>
     * Priority value can range from 1-10, with 1 being the highest priority.
     * </pre>
     *
     * <code>optional int32 value = 1;</code>
     */
    public int getValue() {
      return value_;
    }
    /**
     * <pre>
     * Priority value can range from 1-10, with 1 being the highest priority.
     * </pre>
     *
     * <code>optional int32 value = 1;</code>
     */
    public void setValue(int value) {

      value_ = value;
    }
    /**
     * <pre>
     * Priority value can range from 1-10, with 1 being the highest priority.
     * </pre>
     *
     * <code>optional int32 value = 1;</code>
     */
    private void clearValue() {

      value_ = 0;
    }

  }
}

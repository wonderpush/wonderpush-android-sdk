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

package com.wonderpush.sdk.inappmessaging.internal;

import androidx.annotation.Nullable;
import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks;
import com.wonderpush.sdk.inappmessaging.internal.time.Clock;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.inappmessaging.model.RateLimit;

import java.util.List;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;

public class DisplayCallbacksImpl implements InAppMessagingDisplayCallbacks {

  private final ImpressionStorageClient impressionStorageClient;
  private final Clock clock;
  private final RateLimiterClient rateLimiterClient;
  private final RateLimit appForegroundRateLimit;
  private final MetricsLoggerClient metricsLoggerClient;
  private final InAppMessage inAppMessage;
  private final String triggeringEvent;

  private static boolean wasImpressed;
  private static final String MESSAGE_CLICK = "message click to metrics logger";

  DisplayCallbacksImpl(
      ImpressionStorageClient impressionStorageClient,
      Clock clock,
      RateLimiterClient rateLimiterClient,
      RateLimit appForegroundRateLimit,
      MetricsLoggerClient metricsLoggerClient,
      InAppMessage inAppMessage,
      String triggeringEvent) {
    this.impressionStorageClient = impressionStorageClient;
    this.clock = clock;
    this.rateLimiterClient = rateLimiterClient;
    this.appForegroundRateLimit = appForegroundRateLimit;
    this.metricsLoggerClient = metricsLoggerClient;
    this.inAppMessage = inAppMessage;
    this.triggeringEvent = triggeringEvent;

    // just to be explicit
    wasImpressed = false;
  }

  @Override
  public void impressionDetected() {

    // In the future, when more logAction events are supported, it might be worth
    // extracting this logic into a manager similar to InAppMessageStreamManager
    String MESSAGE_IMPRESSION = "message impression to metrics logger";

    if (shouldLog() && !wasImpressed) {
      Logging.logd("Attempting to record: " + MESSAGE_IMPRESSION);
      Completable logImpressionToMetricsLogger =
              Completable.fromAction(() -> metricsLoggerClient.logImpression(inAppMessage));
      Completable logImpressionCompletable =
              logToImpressionStore()
                      .andThen(logImpressionToMetricsLogger)
                      .andThen(updateWasImpressed());
      logImpressionCompletable.subscribeOn(Schedulers.io()).subscribe();
      return;
    }

    logActionNotTaken(MESSAGE_IMPRESSION);
  }

  private Completable updateWasImpressed() {
    return Completable.fromAction(() -> wasImpressed = true);
  }

  @Override
  public void messageDismissed(InAppMessagingDismissType dismissType) {

    /**
     * NOTE: While the api is passing us the campaign id via the IAM, we pull the campaignId from
     * the cache to ensure that we're only logging events for campaigns that we've fetched - to
     * avoid implicitly trusting an id that is provided through the app
     */
    String MESSAGE_DISMISSAL = "message dismissal to metrics logger";
    if (shouldLog()) {
      Logging.logd("Attempting to record: " + MESSAGE_DISMISSAL);
      Completable completable =
              Completable.fromAction(() -> metricsLoggerClient.logDismiss(inAppMessage, dismissType));
      logImpressionIfNeeded(completable);
      return;
    }
    logActionNotTaken(MESSAGE_DISMISSAL);
  }

  @Override
  public void messageClicked(List<ActionModel> actions) {

  /**
   * NOTE: While the api is passing us the campaign id via the IAM, we pul the campaignId from
   * the cache to ensure that we're only logging events for campaigns that we've fetched - to
   * avoid implicitly trusting an id that is provided through the app
   */
    if (shouldLog()) {
      if (actions.size() == 0) {
        messageDismissed(InAppMessagingDismissType.CLICK);
        return;
      }
      logMessageClick(actions);
      return;
    }
    logActionNotTaken(MESSAGE_CLICK);
  }

  private void logMessageClick(List<ActionModel> actions) {
    Logging.logd("Attempting to record: " + MESSAGE_CLICK);
    Completable completable =
            Completable.fromAction(() -> metricsLoggerClient.logMessageClick(inAppMessage, actions));

    logImpressionIfNeeded(completable);
  }

  private void logMessageClick(String buttonLabel) {
    Logging.logd("Attempting to record: " + MESSAGE_CLICK);
    Completable completable =
        Completable.fromAction(() -> metricsLoggerClient.logMessageClick(inAppMessage, buttonLabel));

    logImpressionIfNeeded(completable);
  }

  @Override
  public void messageClicked(@Nullable String buttonLabel) {
    /**
     * NOTE: While the api is passing us the campaign id via the IAM, we pul the campaignId from
     * the cache to ensure that we're only logging events for campaigns that we've fetched - to
     * avoid implicitly trusting an id that is provided through the app
     */
    if (shouldLog()) {
      logMessageClick(buttonLabel);
      return;
    }
    logActionNotTaken(MESSAGE_CLICK);
  }

  @Override
  public void displayErrorEncountered(InAppMessagingErrorReason errorReason) {
    /**
     * NOTE: While the api is passing us the campaign id via the IAM, we pull the campaignId from
     * the cache to ensure that we're only logging events for campaigns that we've fetched - to
     * avoid implicitly trusting an id that is provided through the app
     */
    String RENDER_ERROR = "render error to metrics logger";
    if (shouldLog()) {
      Logging.logd("Attempting to record: " + RENDER_ERROR);
      Completable completable =
              Completable.fromAction(
                      () -> metricsLoggerClient.logRenderError(inAppMessage, errorReason));
      logToImpressionStore()
              .andThen(completable)
              .andThen(updateWasImpressed())
              .subscribeOn(Schedulers.io()).subscribe();
      return;
    }

    logActionNotTaken(RENDER_ERROR);
  }

  /** We should log if data collection is enabled and the message is not a test message. */
  private boolean shouldLog() {
    return true;
  }

  private void logImpressionIfNeeded(Completable actionToTake) {
    if (!wasImpressed) {
      impressionDetected();
    }

    actionToTake.subscribe();
  }

  /**
   * Logging to clarify why an action was not taken. For example why an impression was not logged.
   * TODO: Refactor this to be a function wrapper.
   *
   * @hide
   */
  private void logActionNotTaken(String action, Maybe<String> reason) {
    // If provided a reason then use that.
    if (reason != null) {
      Logging.logd(String.format("Not recording: %s. Reason: %s", action, reason));
    }
    // If a reason is not provided then check for a test message.
    else if (inAppMessage.getNotificationMetadata().getIsTestMessage()) {
      Logging.logd(String.format("Not recording: %s. Reason: Message is test message", action));
    }
    // If no reason and not a test message check for data collection being disabled.
    else {
      Logging.logd(String.format("Not recording: %s.", action));
    }
  }

  private void logActionNotTaken(String action) {
    logActionNotTaken(action, null);
  }

  private Completable logToImpressionStore() {
    String campaignId = inAppMessage.getNotificationMetadata().getCampaignId();
    Logging.logd(
            "Attempting to record message impression in impression store for id: " + campaignId);
    Completable storeCampaignImpression =
            impressionStorageClient
                    .storeImpression(campaignId)
                    .doOnError(e -> Logging.loge("Impression store write failure:" + e.getMessage()))
                    .doOnComplete(() -> Logging.logd("Impression store write success"));
    if (InAppMessageStreamManager.isAppForegroundEvent(triggeringEvent)
        || InAppMessageStreamManager.isAppLaunchEvent(triggeringEvent)) {
      Completable incrementAppForegroundRateLimit =
              rateLimiterClient
                      .increment(appForegroundRateLimit)
                      .doOnError(e -> Logging.loge("Rate limiter client write failure"))
                      .doOnComplete(() -> Logging.logd("Rate limiter client write success"))
                      .onErrorComplete(); // Absorb rate limiter write errors
      return incrementAppForegroundRateLimit.andThen(storeCampaignImpression);
    }
    return storeCampaignImpression;
  }

}

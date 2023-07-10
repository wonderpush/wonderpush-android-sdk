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

import com.wonderpush.sdk.JSONSyncInstallation;
import com.wonderpush.sdk.PresenceManager;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushConfiguration;
import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.AppForeground;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.ProgrammaticTrigger;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.internal.time.Clock;
import com.wonderpush.sdk.inappmessaging.model.*;
import com.wonderpush.sdk.inappmessaging.model.Campaign;
import com.wonderpush.sdk.inappmessaging.model.CommonTypesProto.TriggeringCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import com.wonderpush.sdk.ratelimiter.RateLimit;
import com.wonderpush.sdk.ratelimiter.RateLimiter;
import com.wonderpush.sdk.segmentation.Segmenter;
import com.wonderpush.sdk.segmentation.parser.*;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Function;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to federate multiple clients and encapsulate the high level behavior of the iam headless
 * sdk
 *
 * @hide
 */
@InAppMessagingScope
public class InAppMessageStreamManager {
  public static final String APP_LAUNCH = "APP_LAUNCH";
  public static final String ON_FOREGROUND = "ON_FOREGROUND";
  private final ConnectableFlowable<EventOccurrence> appForegroundEventFlowable;
  private final ConnectableFlowable<EventOccurrence> programmaticTriggerEventFlowable;
  private final Clock clock;
  private final Schedulers schedulers;
  private final ImpressionStorageClient impressionStorageClient;
  private final RateLimit appForegroundRateLimit;
  private final AnalyticsEventsManager analyticsEventsManager;
  private final TestDeviceHelper testDeviceHelper;
  private final InAppMessaging.InAppMessagingDelegate inAppMessagingDelegate;

  @Inject
  public InAppMessageStreamManager(
          @AppForeground ConnectableFlowable<EventOccurrence> appForegroundEventFlowable,
          @ProgrammaticTrigger ConnectableFlowable<EventOccurrence> programmaticTriggerEventFlowable,
          Clock clock,
          AnalyticsEventsManager analyticsEventsManager,
          Schedulers schedulers,
          ImpressionStorageClient impressionStorageClient,
          @AppForeground RateLimit appForegroundRateLimit,
          TestDeviceHelper testDeviceHelper,
          InAppMessaging.InAppMessagingDelegate inAppMessagingDelegate) {
    this.appForegroundEventFlowable = appForegroundEventFlowable;
    this.programmaticTriggerEventFlowable = programmaticTriggerEventFlowable;
    this.clock = clock;
    this.analyticsEventsManager = analyticsEventsManager;
    this.schedulers = schedulers;
    this.impressionStorageClient = impressionStorageClient;
    this.appForegroundRateLimit = appForegroundRateLimit;
    this.testDeviceHelper = testDeviceHelper;
    this.inAppMessagingDelegate = inAppMessagingDelegate;
  }

  private static boolean containsTriggeringCondition(EventOccurrence event, Campaign campaign) {
    for (TriggeringCondition condition : campaign.getTriggeringConditions()) {
      if (hasIamTrigger(condition, event.eventType)) {
        // Min occurences are not supported for system events on Android
        return true;
      }
      if (hasAnalyticsTrigger(condition, event.eventType)) {
        if (condition.getMinOccurrences() > 0 && condition.getMinOccurrences() > event.allTimeOccurrences) {
          // Count criteria not met, skip to next trigger definition
          continue;
        }
        Logging.logd(String.format("The event %s is contained in the list of triggers", event));
        return true;
      }
    }
    return false;
  }

  private static boolean matchesSegment(Segmenter segmenter, Campaign campaign) {
      // No segment means match all
      if (campaign.getSegment() == null) return true;
      // No segmenter means we can't perform segmentation
      if (segmenter == null) return false;
      try {
          ASTCriterionNode parsedInstallationSegment = Segmenter.parseInstallationSegment(campaign.getSegment());
          return segmenter.matchesInstallation(parsedInstallationSegment);
      } catch (Exception e) {
          Logging.loge(String.format("Could not parse segment %s", campaign.getSegment().toString()), e);
          return false;
      }
  }

  private static long delayForEvent(String event, Campaign campaign) {
      for (TriggeringCondition condition : campaign.getTriggeringConditions()) {
          if (hasIamTrigger(condition, event) || hasAnalyticsTrigger(condition, event)) {
              Logging.logd(String.format("The event %s is contained in the list of triggers", event));
              return condition.getDelay();
          }
      }
      return 0;
  }

  private static boolean hasIamTrigger(TriggeringCondition tc, String event) {
    return tc.getIamTrigger().toString().equals(event);
  }

  private static boolean hasAnalyticsTrigger(TriggeringCondition tc, String event) {
    return tc.getEvent() != null && tc.getEvent().getName() != null && tc.getEvent().getName().equals(event);
  }

  private static boolean isActive(Clock clock, Campaign campaign) {
    long campaignStartTime = campaign.getCampaignStartTimeMillis();
    long campaignEndTime = campaign.getCampaignEndTimeMillis();
    long currentTime = clock.now();
    return currentTime > campaignStartTime && (campaignEndTime == 0 || currentTime < campaignEndTime);
  }

  // Comparisons treat the numeric values of priorities like they were ranks i.e lower is better.
  // If one campaign is a test campaign it is of higher priority.
  // Example: P1 > P2. P2(test) > P1. P1(test) > P2(test)
  private static int compareByPriority(Campaign campaign1, Campaign campaign2) {
    return 0;
  }

  public static boolean isAppLaunchEvent(String event) {
      return event != null && event.equals(APP_LAUNCH);
  }

  public static boolean isAppForegroundEvent(String event) {
    return event != null && event.equals(ON_FOREGROUND);
  }

  public Flowable<TriggeredInAppMessage> createInAppMessageStream() {
    return Flowable.merge(
            appForegroundEventFlowable,
            analyticsEventsManager.getAnalyticsEventsFlowable(),
            programmaticTriggerEventFlowable)
        .doOnNext(e -> Logging.logd("Event Triggered: " + e))
        .observeOn(schedulers.io())
        .concatMap(
            event -> {

              Function<Campaign, Maybe<Campaign>> filterTooImpressed =
                  campaign ->
                          impressionStorageClient
                            .isCapped(campaign)
                            .doOnError(
                                e ->
                                    Logging.logw("Impression store read fail: " + e.getMessage()))
                            .onErrorResumeNext(
                                Single.just(false)) // Absorb impression read errors
                            .doOnSuccess(isCapped -> logCappedStatus(campaign, isCapped))
                            .filter(isCapped -> !isCapped)
                            .map(isCapped -> campaign);

              Function<Campaign, Maybe<Campaign>> appForegroundRateLimitFilter =
                  content -> getContentIfNotRateLimited(event.eventType, content);

              Function<Campaign, Maybe<Campaign>> filterDisplayable =
                  campaign -> {
                    if (campaign.getContent() != null) return Maybe.just(campaign);
                      Logging.logd("Filtering non-displayable message");
                      return Maybe.empty();
                  };

              Function<List<Campaign>, Maybe<TriggeredInAppMessage>>
                  selectCampaign =
                      response ->
                          getTriggeredInAppMessageMaybe(
                              event,
                              filterTooImpressed,
                              appForegroundRateLimitFilter,
                              filterDisplayable,
                              response);

              Maybe<List<Campaign>> serviceFetch =
                      Maybe.<List<Campaign>>create(
                              emitter -> {
                                  inAppMessagingDelegate.fetchInAppConfig((JSONObject config, Throwable error) -> {
                                      try {
                                          if (error != null) emitter.onError(error);
                                          else {
                                              JSONArray campaignsJson = config != null ? config.optJSONArray("campaigns") : null;
                                              List<Campaign> messages = new ArrayList<>();
                                              for (int i = 0; campaignsJson != null && i < campaignsJson.length(); i++) {
                                                  JSONObject campaignJson = campaignsJson.optJSONObject(i);
                                                  if (campaignJson == null) continue;
                                                  Campaign campaign = Campaign.fromJSON(campaignJson);
                                                  if (campaign != null) messages.add(campaign);
                                              }
                                              emitter.onSuccess(messages);
                                          }
                                          emitter.onComplete();
                                      } catch (Throwable t) {
                                          emitter.onError(t);
                                      }
                                  });
                              })
                              .doOnSuccess(
                                      resp ->
                                              Logging.logi(
                                                      String.format(
                                                              Locale.US,
                                                              "Successfully fetched %d messages from backend",
                                                              resp.size())))
                              .doOnSuccess(analyticsEventsManager::updateContextualTriggers)
                              //.doOnSuccess(abtIntegrationHelper::updateRunningExperiments)
                              .doOnSuccess(testDeviceHelper::processCampaignFetch)
                              .doOnError(e -> Logging.loge("Service fetch error: ", e))
                              .onErrorResumeNext(Maybe.empty()); // Absorb service failures

              return serviceFetch
                      .flatMap(selectCampaign)
                      .toFlowable();
            })
        .observeOn(schedulers.mainThread()); // Updates are delivered on the main thread
  }

  private Maybe<Campaign> getContentIfNotRateLimited(String event, Campaign campaign) {
    if (isAppForegroundEvent(event) || isAppLaunchEvent(event)) {
        try {
            RateLimiter limiter = RateLimiter.getInstance();
            if (limiter.isRateLimited(appForegroundRateLimit)) {
                return Maybe.empty();
            }
        } catch (RateLimiter.MissingSharedPreferencesException e) {
            Logging.loge("Could not get rate limiter", e);
        }
    }
    return Maybe.just(campaign);
  }

  private static void logCappedStatus(Campaign campaign, Boolean isCapped) {
      Logging.logi(
              String.format(
                      "Campaign %s capped ? : %s",
                      campaign.getNotificationMetadata().getCampaignId(), isCapped));
  }

  private Maybe<TriggeredInAppMessage> getTriggeredInAppMessageMaybe(
          EventOccurrence event,
          Function<Campaign, Maybe<Campaign>> filterAlreadyImpressed,
          Function<Campaign, Maybe<Campaign>> appForegroundRateLimitFilter,
          Function<Campaign, Maybe<Campaign>> filterDisplayable,
          List<Campaign> campaigns) {
    Segmenter.Data segmenterData = null;
    try {
      JSONObject installation = JSONSyncInstallation.forCurrentUser().getSdkState();
      installation.putOpt("userId", WonderPush.getUserId());

      // Tracked events
      List<JSONObject> trackedEvents = WonderPushConfiguration.getTrackedEvents();

      // Presence info
      PresenceManager.PresencePayload lastPresencePayload = inAppMessagingDelegate.getPresenceManager().getLastPresencePayload();
      Segmenter.PresenceInfo presenceInfo = lastPresencePayload == null ? null : new Segmenter.PresenceInfo(lastPresencePayload.getFromDate().getTime(), lastPresencePayload.getUntilDate().getTime(), lastPresencePayload.getElapsedTime());

      // Build segmenter data
      segmenterData = new Segmenter.Data(installation, trackedEvents, presenceInfo, WonderPushConfiguration.getLastAppOpenDate());
    } catch (JSONException e) {
      Logging.loge("Could not create segmenter data", e);
    }
    final Segmenter segmenter = segmenterData == null ? null : new Segmenter(segmenterData);
    return Flowable.fromIterable(campaigns)
        .filter(campaign -> isActive(clock, campaign))
        .filter(campaign -> containsTriggeringCondition(event, campaign))
        .filter(campaign -> matchesSegment(segmenter, campaign))
        .flatMapMaybe(filterAlreadyImpressed)
        .flatMapMaybe(appForegroundRateLimitFilter)
        .flatMapMaybe(filterDisplayable)
        .sorted(InAppMessageStreamManager::compareByPriority)
        .firstElement()
        .flatMap(campaign -> triggeredInAppMessage(campaign, event.eventType, delayForEvent(event.eventType, campaign)));
  }

  private Maybe<TriggeredInAppMessage> triggeredInAppMessage(Campaign campaign, String event, long delay) {
    InAppMessage inAppMessage = campaign.getContent();
    if (inAppMessage.getMessageType() == null || inAppMessage.getMessageType().equals(MessageType.UNSUPPORTED)) {
      return Maybe.empty();
    }

    return Maybe.just(new TriggeredInAppMessage(inAppMessage, event, delay));
  }
}

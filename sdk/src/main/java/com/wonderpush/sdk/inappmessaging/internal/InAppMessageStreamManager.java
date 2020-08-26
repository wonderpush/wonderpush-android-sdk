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

import com.google.android.gms.tasks.Task;
import com.wonderpush.sdk.JSONSyncInstallationCustom;
import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.WonderPushConfiguration;
import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.AppForeground;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.ProgrammaticTrigger;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.internal.time.Clock;
import com.wonderpush.sdk.inappmessaging.model.*;
import com.wonderpush.sdk.inappmessaging.model.Campaign.ThickContent;
import com.wonderpush.sdk.inappmessaging.model.CommonTypesProto.TriggeringCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import com.wonderpush.sdk.segmentation.Segmenter;
import com.wonderpush.sdk.segmentation.parser.*;
import com.wonderpush.sdk.segmentation.parser.criteria.UnknownCriterionError;
import com.wonderpush.sdk.segmentation.parser.datasource.InstallationSource;
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
  public static final String ON_FOREGROUND = "ON_FOREGROUND";
  private final ConnectableFlowable<String> appForegroundEventFlowable;
  private final ConnectableFlowable<String> programmaticTriggerEventFlowable;
  private final Clock clock;
  private final Schedulers schedulers;
  private final ImpressionStorageClient impressionStorageClient;
  private final RateLimiterClient rateLimiterClient;
  private final RateLimit appForegroundRateLimit;
  private final AnalyticsEventsManager analyticsEventsManager;
  private final TestDeviceHelper testDeviceHelper;
  private final InAppMessaging.InAppMessagingConfiguration inAppMessagingConfiguration;

  @Inject
  public InAppMessageStreamManager(
          @AppForeground ConnectableFlowable<String> appForegroundEventFlowable,
          @ProgrammaticTrigger ConnectableFlowable<String> programmaticTriggerEventFlowable,
          Clock clock,
          AnalyticsEventsManager analyticsEventsManager,
          Schedulers schedulers,
          ImpressionStorageClient impressionStorageClient,
          RateLimiterClient rateLimiterClient,
          @AppForeground RateLimit appForegroundRateLimit,
          TestDeviceHelper testDeviceHelper,
          InAppMessaging.InAppMessagingConfiguration inAppMessagingConfiguration) {
    this.appForegroundEventFlowable = appForegroundEventFlowable;
    this.programmaticTriggerEventFlowable = programmaticTriggerEventFlowable;
    this.clock = clock;
    this.analyticsEventsManager = analyticsEventsManager;
    this.schedulers = schedulers;
    this.impressionStorageClient = impressionStorageClient;
    this.rateLimiterClient = rateLimiterClient;
    this.appForegroundRateLimit = appForegroundRateLimit;
    this.testDeviceHelper = testDeviceHelper;
    this.inAppMessagingConfiguration = inAppMessagingConfiguration;
  }

  private static boolean containsTriggeringCondition(String event, ThickContent content) {
    if (isAppForegroundEvent(event) && content.getIsTestCampaign()) {
      return true; // the triggering condition for test campaigns is always 'app foreground'
    }
    for (TriggeringCondition condition : content.getTriggeringConditionsList()) {
      if (hasIamTrigger(condition, event) || hasAnalyticsTrigger(condition, event)) {
        Logging.logd(String.format("The event %s is contained in the list of triggers", event));
        return true;
      }
    }
    return false;
  }

  private static boolean matchesSegment(Segmenter segmenter, ThickContent content) {
      // No segment means match all
      if (content.getSegment() == null) return true;
      // No segmenter means we can't perform segmentation
      if (segmenter == null) return false;
      try {
          ASTCriterionNode parsedInstallationSegment = Segmenter.parseInstallationSegment(content.getSegment());
          return segmenter.matchesInstallation(parsedInstallationSegment);
      } catch (Exception e) {
          Logging.loge(String.format("Could not parse segment %s", content.getSegment().toString()), e);
          return false;
      }
  }

  private static long delayForEvent(String event, ThickContent content) {
      for (TriggeringCondition condition : content.getTriggeringConditionsList()) {
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

  private static boolean isActive(Clock clock, ThickContent content) {
    long campaignStartTime;
    long campaignEndTime;
    if (content.getPayloadCase().equals(ThickContent.PayloadCase.VANILLA_PAYLOAD)) {
      // Handle the campaign case
      campaignStartTime = content.getVanillaPayload().getCampaignStartTimeMillis();
      campaignEndTime = content.getVanillaPayload().getCampaignEndTimeMillis();
    } else if (content.getPayloadCase().equals(ThickContent.PayloadCase.EXPERIMENTAL_PAYLOAD)) {
      // Handle the experiment case
      campaignStartTime = content.getExperimentalPayload().getCampaignStartTimeMillis();
      campaignEndTime = content.getExperimentalPayload().getCampaignEndTimeMillis();
    } else {
      // If we have no valid payload then don't display
      return false;
    }
    long currentTime = clock.now();
    return currentTime > campaignStartTime && (campaignEndTime == 0 || currentTime < campaignEndTime);
  }

  // Comparisons treat the numeric values of priorities like they were ranks i.e lower is better.
  // If one campaign is a test campaign it is of higher priority.
  // Example: P1 > P2. P2(test) > P1. P1(test) > P2(test)
  private static int compareByPriority(ThickContent content1, ThickContent content2) {
    if (content1.getIsTestCampaign() && !content2.getIsTestCampaign()) {
      return -1;
    }
    if (content2.getIsTestCampaign() && !content1.getIsTestCampaign()) {
      return 1;
    }
    return Integer.compare(content1.getPriority().getValue(), content2.getPriority().getValue());
  }

  public static boolean isAppForegroundEvent(TriggeringCondition event) {
    return event.getIamTrigger().toString().equals(ON_FOREGROUND);
  }

  public static boolean isAppForegroundEvent(String event) {
    return event.equals(ON_FOREGROUND);
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

              Function<ThickContent, Maybe<ThickContent>> filterAlreadyImpressed =
                  content ->
                      content.getIsTestCampaign()
                          ? Maybe.just(content)
                          : impressionStorageClient
                              .isImpressed(content)
                              .doOnError(
                                  e ->
                                      Logging.logw("Impression store read fail: " + e.getMessage()))
                              .onErrorResumeNext(
                                  Single.just(false)) // Absorb impression read errors
                              .doOnSuccess(isImpressed -> logImpressionStatus(content, isImpressed))
                              .filter(isImpressed -> !isImpressed)
                              .map(isImpressed -> content);

              Function<ThickContent, Maybe<ThickContent>> appForegroundRateLimitFilter =
                  content -> getContentIfNotRateLimited(event, content);

              Function<ThickContent, Maybe<ThickContent>> filterDisplayable =
                  thickContent -> {
                    switch (thickContent.getContent().getMessageDetailsCase()) {
                      case BANNER:
                      case IMAGE_ONLY:
                      case MODAL:
                      case CARD:
                        return Maybe.just(thickContent);
                      default:
                        Logging.logd("Filtering non-displayable message");
                        return Maybe.empty();
                    }
                  };

              Function<List<ThickContent>, Maybe<TriggeredInAppMessage>>
                  selectThickContent =
                      response ->
                          getTriggeredInAppMessageMaybe(
                              event,
                              filterAlreadyImpressed,
                              appForegroundRateLimitFilter,
                              filterDisplayable,
                              response);

              Maybe<CampaignImpressionList> alreadySeenCampaigns =
                  impressionStorageClient
                      .getAllImpressions()
                      .doOnError(
                          e -> Logging.logw("Impressions store read fail: " + e.getMessage()))
                      .defaultIfEmpty(new CampaignImpressionList())
                      .onErrorResumeNext(Maybe.just(new CampaignImpressionList()));

              Function<CampaignImpressionList, Maybe<List<Campaign.ThickContent>>> serviceFetch =
                  impressions ->
                          Maybe.<List<Campaign.ThickContent>>create(
                                  emitter -> {
                                      inAppMessagingConfiguration.fetchInAppConfig((JSONObject config, Throwable error) -> {
                                          try {
                                              if (error != null) emitter.onError(error);
                                              else {
                                                  JSONArray campaignsJson = config != null ? config.optJSONArray("campaigns") : null;
                                                  List<Campaign.ThickContent> messages = new ArrayList<>();
                                                  for (int i = 0; campaignsJson != null && i < campaignsJson.length(); i++) {
                                                      JSONObject campaignJson = campaignsJson.optJSONObject(i);
                                                      if (campaignJson == null) continue;
                                                      Campaign.ThickContent thickContent = Campaign.ThickContent.fromJSON(campaignJson);
                                                      if (thickContent != null) messages.add(thickContent);
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

              return alreadySeenCampaigns
                      .flatMap(serviceFetch)
                      .flatMap(selectThickContent)
                      .toFlowable();
            })
        .observeOn(schedulers.mainThread()); // Updates are delivered on the main thread
  }

  private Maybe<ThickContent> getContentIfNotRateLimited(String event, ThickContent content) {
    if (!content.getIsTestCampaign() && isAppForegroundEvent(event)) {
      return rateLimiterClient
          .isRateLimited(appForegroundRateLimit)
          .doOnSuccess(
              isRateLimited -> Logging.logi("App foreground rate limited ? : " + isRateLimited))
          .onErrorResumeNext(Single.just(false)) // Absorb rate limit errors
          .filter(isRateLimited -> !isRateLimited)
          .map(isRateLimited -> content);
    }
    return Maybe.just(content);
  }

  private static void logImpressionStatus(ThickContent content, Boolean isImpressed) {
    if (content.getPayloadCase().equals(ThickContent.PayloadCase.VANILLA_PAYLOAD)) {
      Logging.logi(
          String.format(
              "Already impressed campaign %s ? : %s",
              content.getVanillaPayload().getCampaignId(), isImpressed));
    } else if (content.getPayloadCase().equals(ThickContent.PayloadCase.EXPERIMENTAL_PAYLOAD)) {
      Logging.logi(
          String.format(
              "Already impressed experiment %s ? : %s",
              content.getExperimentalPayload().getCampaignId(), isImpressed));
    }
  }

  private Maybe<TriggeredInAppMessage> getTriggeredInAppMessageMaybe(
          String event,
          Function<ThickContent, Maybe<ThickContent>> filterAlreadyImpressed,
          Function<ThickContent, Maybe<ThickContent>> appForegroundRateLimitFilter,
          Function<ThickContent, Maybe<ThickContent>> filterDisplayable,
          List<Campaign.ThickContent> messages) {
    Segmenter.Data segmenterData = null;
    try {
      // Get custom props
      JSONObject customProperties = JSONSyncInstallationCustom.forCurrentUser().getSdkState();
      // Get core props
      String installationCorePropertiesString = WonderPushConfiguration.getCachedInstallationCoreProperties();

      // Build the installation JSONObject
      JSONObject installation = installationCorePropertiesString == null ? new JSONObject() : new JSONObject(installationCorePropertiesString);
      if (customProperties != null) installation.put("custom", customProperties);

      // Tracked events
      List<JSONObject> trackedEvents = WonderPushConfiguration.getTrackedEvents();

      // Build segmenter data
      segmenterData = new Segmenter.Data(installation, trackedEvents,null, WonderPushConfiguration.getLastAppOpenDate());
    } catch (JSONException e) {
      Logging.loge("Could not create segmenter data", e);
    }
    final Segmenter segmenter = segmenterData == null ? null : new Segmenter(segmenterData);
    return Flowable.fromIterable(messages)
        .filter(content -> testDeviceHelper.isDeviceInTestMode() || isActive(clock, content))
        .filter(content -> containsTriggeringCondition(event, content))
        .filter(content -> matchesSegment(segmenter, content))
        .flatMapMaybe(filterAlreadyImpressed)
        .flatMapMaybe(appForegroundRateLimitFilter)
        .flatMapMaybe(filterDisplayable)
        .sorted(InAppMessageStreamManager::compareByPriority)
        .firstElement()
        .flatMap(content -> triggeredInAppMessage(content, event, delayForEvent(event, content)));
  }

  private Maybe<TriggeredInAppMessage> triggeredInAppMessage(ThickContent content, String event, long delay) {
    String campaignId;
    String notificationId;
    String viewId;
    if (content.getPayloadCase().equals(ThickContent.PayloadCase.VANILLA_PAYLOAD)) {
      // Handle vanilla campaign case
      campaignId = content.getVanillaPayload().getCampaignId();
      notificationId = content.getVanillaPayload().getNotificationId();
      viewId = content.getVanillaPayload().getViewId();
    } else {
      return Maybe.empty();
    }
    InAppMessage inAppMessage =
        ProtoMarshallerClient.decode(
            content.getContent(),
            campaignId,
            notificationId,
            viewId,
            content.getIsTestCampaign());
    if (inAppMessage.getMessageType().equals(MessageType.UNSUPPORTED)) {
      return Maybe.empty();
    }

    return Maybe.just(new TriggeredInAppMessage(inAppMessage, event, delay));
  }

  private static <T> Maybe<T> taskToMaybe(Task<T> task) {
    return Maybe.create(
        emitter -> {
          task.addOnSuccessListener(
              result -> {
                emitter.onSuccess(result);
                emitter.onComplete();
              });
          task.addOnFailureListener(
              e -> {
                emitter.onError(e);
                emitter.onComplete();
              });
        });
  }
}

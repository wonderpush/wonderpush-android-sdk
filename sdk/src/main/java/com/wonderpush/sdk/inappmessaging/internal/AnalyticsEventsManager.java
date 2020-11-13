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

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.wonderpush.sdk.WonderPush;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.model.Campaign;
import com.wonderpush.sdk.inappmessaging.model.CommonTypesProto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.flowables.ConnectableFlowable;

/**
 * Container for the analytics handler as well as the flowable used to act on emitted events
 *
 * @hide
 */
@InAppMessagingScope
public class AnalyticsEventsManager {
  private final ConnectableFlowable<String> flowable;
  private Set<String> analyticsEventNames;
  private Application application;

  @Inject
  public AnalyticsEventsManager(Application application) {
    this.application = application;
    AnalyticsFlowableSubscriber subscriber = new AnalyticsFlowableSubscriber();
    flowable = Flowable.<String>create(subscriber, BackpressureStrategy.BUFFER).publish();

    // We ignore the subscription since this connected flowable is expected to last the lifetime of
    // the app, but this calls the 'subscribe' method of the subscriber, which registers the handle
    flowable.connect();
  }

  public ConnectableFlowable<String> getAnalyticsEventsFlowable() {
    return flowable;
  }

  //@VisibleForTesting
  static Set<String> extractAnalyticsEventNames(List<Campaign> campaigns) {
    Set<String> analyticsEvents = new HashSet<>();
    for (Campaign campaign : campaigns) {
      for (CommonTypesProto.TriggeringCondition condition : campaign.getTriggeringConditions()) {
        if (condition.getEvent() != null && !TextUtils.isEmpty(condition.getEvent().getName())) {
          analyticsEvents.add(condition.getEvent().getName());
        }
      }
    }
    return analyticsEvents;
  }

  public void updateContextualTriggers(List<Campaign> campaigns) {
    Logging.logd(
            "Updating contextual triggers for the following analytics events: " + analyticsEventNames);
    analyticsEventNames = extractAnalyticsEventNames(campaigns);
  }

  private class AnalyticsFlowableSubscriber implements FlowableOnSubscribe<String> {

    AnalyticsFlowableSubscriber() {}

    @Override
    public void subscribe(FlowableEmitter<String> emitter) {
      Logging.logd("Subscribing to analytics events.");
      LocalBroadcastManager.getInstance(application)
              .registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                  String eventType = intent.getStringExtra(WonderPush.INTENT_EVENT_TRACKED_EVENT_TYPE);
                  emitter.onNext(eventType);
                }
              }, new IntentFilter(WonderPush.INTENT_EVENT_TRACKED));
    }
  }
}

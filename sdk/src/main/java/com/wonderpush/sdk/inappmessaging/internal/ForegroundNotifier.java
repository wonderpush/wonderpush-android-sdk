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

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import io.reactivex.BackpressureStrategy;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.subjects.BehaviorSubject;

import static com.wonderpush.sdk.inappmessaging.internal.InAppMessageStreamManager.APP_LAUNCH;
import static com.wonderpush.sdk.inappmessaging.internal.InAppMessageStreamManager.ON_FOREGROUND;

import androidx.annotation.NonNull;

import com.wonderpush.sdk.WonderPushCompatibilityHelper;
import com.wonderpush.sdk.inappmessaging.model.EventOccurrence;

/**
 * The {@link ForegroundNotifier} notifies listeners via {@link #foregroundFlowable()} when an
 * application comes to the foreground.
 *
 * <p>Supported foreground scenarios
 *
 * <ul>
 *   <li>App resumed phone screen is unlocked
 *   <li>App starts when app icon is clicked
 *   <li>App resumes after completion of phone call
 *   <li>App is chosen from recent apps menu
 *   <li>A routing activity won't fire an event until we land on a normal activity, to support
 *       splash screens
 *   <li>A routing activity between two normal activities within the app navigation won't fire a
 *       spurious foreground event
 * </ul>
 *
 * <p>This works as follows
 *
 * <ul>
 *   <li>When an app is foregrounded for the first time after app icon is clicked, it is moved to
 *       the foreground state and an event is published
 *   <li>When any activity in the app is paused and {@link #onActivityPaused(Activity)} callback is
 *       received, the app is considered to be paused until the next activity starts and the {@link
 *       #onActivityResumed(Activity)} callback is received. A runnable is simultaneously scheduled
 *       to be run after a {@link #DELAY_MILLIS} which will put the app into background state.
 *   <li>If some other activity subsequently starts and beats execution of the runnable by invoking
 *       the {@link #onActivityResumed(Activity)}, the app never went out of view for the user and
 *       is considered to have never gone to the background. The runnable is removed and the app
 *       remains in the foreground.
 *   <li>Similar to the first step, an event is published in the {@link
 *       #onActivityResumed(Activity)} callback if the app was deemed to be in the background.
 *   <li>Events are held back from firing while on routing activities, identified using a metadata.
 * </ul>
 *
 * @hide
 */
public class ForegroundNotifier implements Application.ActivityLifecycleCallbacks {
  public static final long DELAY_MILLIS = 1000;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean heldBackAppLaunch = false;
  private boolean heldBackForeground = false;
  private boolean foreground = false;
  private boolean paused = true;
  private boolean firstActivity = true;
  private Runnable check;
  private final BehaviorSubject<EventOccurrence> foregroundSubject = BehaviorSubject.create();

  /** @return a {@link ConnectableFlowable} representing a stream of foreground events */
  public ConnectableFlowable<EventOccurrence> foregroundFlowable() {
    return foregroundSubject.toFlowable(BackpressureStrategy.BUFFER).publish();
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {
    paused = false;
    boolean wasBackground = !foreground;
    foreground = true;

    if (check != null) {
      handler.removeCallbacks(check);
    }

    boolean holdBack = false;
    try {
      Bundle activityInfoMetaData = WonderPushCompatibilityHelper.getActivityInfoMetaData(activity);
      Object resValue = activityInfoMetaData == null ? null : WonderPushCompatibilityHelper.bundleGetTypeUnsafe(activityInfoMetaData, "com.wonderpush.sdk.iam.ignoreForeground");
      if (resValue instanceof Boolean) {
        holdBack = (Boolean) resValue;
      } else if ("true".equals(resValue) || "false".equals(resValue)) {
        holdBack = "true".equals(resValue);
      }
    } catch (PackageManager.NameNotFoundException e) {
      Logging.logw("could not resolve activity info in ForegroundNotifier: " + e.getMessage());
    }
    if (holdBack) {
      Logging.logi("holding app launch and foreground in-app triggers while on activity " + activity.getComponentName().getShortClassName());
    }

    boolean doAppLaunch = !holdBack && heldBackAppLaunch;
    boolean doForeground = !holdBack && heldBackForeground;
    if (wasBackground) {
      // Detect app launch
      if (firstActivity) {
        firstActivity = false;
        heldBackAppLaunch = holdBack;
        doAppLaunch = !holdBack;
      }
      heldBackForeground = holdBack;
      doForeground = !holdBack;
    }

    // Trigger an app launch before a foreground event
    if (doAppLaunch) {
      heldBackAppLaunch = false;
      Logging.logi("app launch");
      EventOccurrence event = new EventOccurrence();
      event.eventType = APP_LAUNCH;
      foregroundSubject.onNext(event);
    }
    if (doForeground) {
      heldBackForeground = false;
      Logging.logi("went foreground");
      EventOccurrence event = new EventOccurrence();
      event.eventType = ON_FOREGROUND;
      foregroundSubject.onNext(event);
    }
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {
    paused = true;

    if (check != null) {
      handler.removeCallbacks(check);
    }

    handler.postDelayed(
        check = this::delayedBackgroundCheck, DELAY_MILLIS);
  }

  private void delayedBackgroundCheck() {
    if (paused) {
      foreground = false;
      heldBackAppLaunch = false;
      heldBackForeground = false;
    }
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

  @Override
  public void onActivityStarted(@NonNull Activity activity) {}

  @Override
  public void onActivityStopped(@NonNull Activity activity) {}

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {}
}

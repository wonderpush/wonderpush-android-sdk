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

package com.wonderpush.sdk.inappmessaging;

import android.app.Application;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.wonderpush.sdk.InternalEventTracker;
import com.wonderpush.sdk.inappmessaging.internal.DeveloperListenerManager;
import com.wonderpush.sdk.inappmessaging.internal.DisplayCallbacksFactory;
import com.wonderpush.sdk.inappmessaging.internal.InAppMessageStreamManager;
import com.wonderpush.sdk.inappmessaging.internal.Logging;
import com.wonderpush.sdk.inappmessaging.internal.ProgramaticContextualTriggers;
import com.wonderpush.sdk.inappmessaging.internal.injection.components.AppComponent;
import com.wonderpush.sdk.inappmessaging.internal.injection.components.DaggerAppComponent;
import com.wonderpush.sdk.inappmessaging.internal.injection.components.DaggerUniversalComponent;
import com.wonderpush.sdk.inappmessaging.internal.injection.components.UniversalComponent;
import com.wonderpush.sdk.inappmessaging.internal.injection.modules.*;
import com.wonderpush.sdk.inappmessaging.internal.injection.qualifiers.ProgrammaticTrigger;
import com.wonderpush.sdk.inappmessaging.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.model.TriggeredInAppMessage;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;
import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import org.json.JSONObject;

/**
 * The entry point of the In App Messaging headless SDK.
 *
 * <p>In-App Messaging will automatically initialize, and start listening for events.
 *
 */
@InAppMessagingScope
public class InAppMessaging {

  private final InAppMessageStreamManager inAppMessageStreamManager;
  private final DisplayCallbacksFactory displayCallbacksFactory;
  private final DeveloperListenerManager developerListenerManager;
  private final ProgramaticContextualTriggers programaticContextualTriggers;

  private boolean areMessagesSuppressed;
  private InAppMessagingDisplay iamDisplay;

  private static UniversalComponent universalComponent;
  private static AppComponent appComponent;
  private static InAppMessaging instance;

  @Inject
  InAppMessaging(
      InAppMessageStreamManager inAppMessageStreamManager,
      @ProgrammaticTrigger ProgramaticContextualTriggers programaticContextualTriggers,
      DisplayCallbacksFactory displayCallbacksFactory,
      DeveloperListenerManager developerListenerManager) {
    this.inAppMessageStreamManager = inAppMessageStreamManager;
    this.programaticContextualTriggers = programaticContextualTriggers;
    this.areMessagesSuppressed = false;
    this.displayCallbacksFactory = displayCallbacksFactory;
    this.developerListenerManager = developerListenerManager;

    Logging.logi("Starting InAppMessaging runtime");

    Disposable unused =
        inAppMessageStreamManager
            .createInAppMessageStream()
            .subscribe(InAppMessaging.this::triggerInAppMessage);
  }

  private static UniversalComponent initializeUniversalComponent(Application application, InternalEventTracker internalEventTracker) {
    if (universalComponent == null) {
      universalComponent = DaggerUniversalComponent.builder()
              .internalEventTrackerModule(new InternalEventTrackerModule(internalEventTracker))
              .applicationModule(new ApplicationModule(application))
              .programmaticContextualTriggerFlowableModule(new ProgrammaticContextualTriggerFlowableModule(new ProgramaticContextualTriggers()))
              .build();
    }
    return universalComponent;
  }

  private static AppComponent initializeAppComponent(Application application, InternalEventTracker internalEventTracker) {
    if (appComponent == null) {
      appComponent = DaggerAppComponent.builder()
              .universalComponent(initializeUniversalComponent(application, internalEventTracker))
              .build();
    }
    return appComponent;
  }

  public interface JSONObjectHandler {
    void handle(@Nullable JSONObject jsonObject, @Nullable Throwable error);
  }

  public interface InAppMessagingConfiguration {
    boolean inAppViewedReceipts();
    void fetchInAppConfig(JSONObjectHandler handler);
  }

  /**
   * Internal method.
   */
  @NonNull
  @Keep
  public static InAppMessaging initialize(Application application,
                                          InternalEventTracker internalEventTracker,
                                          InAppMessagingConfiguration configuration) {
    if (ConfigurationModule.getInstance() == null) ConfigurationModule.setInstance(configuration);
    if (instance == null) {
      instance = initializeAppComponent(application, internalEventTracker).providesInAppMessaging();
    }
    return instance;
  }

  /**
   * Get InAppMessaging instance
   */
  public static InAppMessaging getInstance() {
    return instance;
  }

  /**
   * Enable or disable suppression of In App Messaging messages
   *
   * <p>When enabled, no in app messages will be rendered until either you either disable
   * suppression, or the app restarts, as this state is not preserved over app restarts.
   *
   * <p>By default, messages are not suppressed.
   *
   * @param areMessagesSuppressed Whether messages should be suppressed
   */
  @Keep
  public void setMessagesSuppressed(@NonNull Boolean areMessagesSuppressed) {
    this.areMessagesSuppressed = areMessagesSuppressed;
  }

  /**
   * Determine whether messages are suppressed or not. This is honored by the UI sdk, which handles
   * rendering the in app message.
   *
   * @return true if messages should be suppressed
   */
  @Keep
  public boolean areMessagesSuppressed() {
    return areMessagesSuppressed;
  }

  /**
   * Returns the current display component.
   * Unless you've set your own with {@link #setMessageDisplayComponent(InAppMessagingDisplay)}, this will be the built-in display component.
   * @return The current display component.
   */
  public @Nullable InAppMessagingDisplay getMessageDisplayComponent() {
    return this.iamDisplay;
  }

  /**
   * Called to set a new message display component for IAM SDK. This is the method used
   * by both the default IAM display SDK or any app wanting to customize the message
   * display.
   */
  @Keep
  public void setMessageDisplayComponent(@NonNull InAppMessagingDisplay messageDisplay) {
    Logging.logi("Setting display event component");
    this.iamDisplay = messageDisplay;
  }

  /**
   * Remove previously registered display listeners.
   */
  @Keep
  public void clearDisplayListener() {
    Logging.logi("Removing display event component");
    this.iamDisplay = null;
  }

  /*
   * Adds/Removes the event listeners. These listeners are triggered after IAM's internal metrics reporting, but regardless of success/failure of the IAM-internal callbacks.
   */

  /**
   * Registers an impression listener with IAM, which will be notified on every IAM impression
   *
   * @param impressionListener
   */
  public void addImpressionListener(
      @NonNull InAppMessagingImpressionListener impressionListener) {
    developerListenerManager.addImpressionListener(impressionListener);
  }

  /**
   * Registers a click listener with IAM, which will be notified on every IAM click
   *
   * @param clickListener
   */
  public void addClickListener(@NonNull InAppMessagingClickListener clickListener) {
    developerListenerManager.addClickListener(clickListener);
  }

  /**
   * Registers a display error listener with IAM, which will be notified on every IAM display
   * error
   *
   * @param displayErrorListener
   */
  public void addDisplayErrorListener(
      @NonNull InAppMessagingDisplayErrorListener displayErrorListener) {
    developerListenerManager.addDisplayErrorListener(displayErrorListener);
  }

  // Executed with provided executor

  /**
   * Registers an impression listener with IAM, which will be notified on every IAM impression,
   * and triggered on the provided executor
   *
   * @param impressionListener
   * @param executor
   */
  public void addImpressionListener(
      @NonNull InAppMessagingImpressionListener impressionListener,
      @NonNull Executor executor) {
    developerListenerManager.addImpressionListener(impressionListener, executor);
  }

  /**
   * Registers a click listener with IAM, which will be notified on every IAM click, and triggered
   * on the provided executor
   *
   * @param clickListener
   * @param executor
   */
  public void addClickListener(
          @NonNull InAppMessagingClickListener clickListener, @NonNull Executor executor) {
    developerListenerManager.addClickListener(clickListener, executor);
  }

  /**
   * Registers a display error listener with IAM, which will be notified on every IAM display
   * error, and triggered on the provided executor
   *
   * @param displayErrorListener
   * @param executor
   */
  public void addDisplayErrorListener(
      @NonNull InAppMessagingDisplayErrorListener displayErrorListener,
      @NonNull Executor executor) {
    developerListenerManager.addDisplayErrorListener(displayErrorListener, executor);
  }

  // Removing individual listeners:

  /**
   * Unregisters an impression listener
   *
   * @param impressionListener
   */
  public void removeImpressionListener(
      @NonNull InAppMessagingImpressionListener impressionListener) {
    developerListenerManager.removeImpressionListener(impressionListener);
  }

  /**
   * Unregisters a click listener
   *
   * @param clickListener
   */
  public void removeClickListener(@NonNull InAppMessagingClickListener clickListener) {
    developerListenerManager.removeClickListener(clickListener);
  }

  /**
   * Unregisters a display error listener
   *
   * @param displayErrorListener
   */
  public void removeDisplayErrorListener(
      @NonNull InAppMessagingDisplayErrorListener displayErrorListener) {
    developerListenerManager.removeDisplayErrorListener(displayErrorListener);
  }

  /**
   * Tell the in-app messaging SDK that an event of the given type was triggered.
   * Useful for testing.
   *
   * @param eventType
   */
  public void triggerEvent(String eventType) {
    programaticContextualTriggers.triggerEvent(eventType);
  }

  private void triggerInAppMessage(TriggeredInAppMessage inAppMessage) {
    if (this.iamDisplay != null) {
      InAppMessagingDisplayCallbacks displayCallbacks = displayCallbacksFactory.generateDisplayCallback(
              inAppMessage.getInAppMessage(), inAppMessage.getTriggeringEvent());
      boolean handled = iamDisplay.displayMessage(
          inAppMessage.getInAppMessage(),
          displayCallbacks,
          inAppMessage.getDelay());
      if (!handled) {
        com.wonderpush.sdk.inappmessaging.display.InAppMessagingDisplay defaultImplementation = com.wonderpush.sdk.inappmessaging.display.InAppMessagingDisplay.getInstance();
        if (defaultImplementation != null) {
          InAppMessagingDisplay defaultDisplay = defaultImplementation.getDefaultInAppMessagingDisplay();
          if (defaultDisplay != null) {
            defaultDisplay.displayMessage(
                    inAppMessage.getInAppMessage(),
                    displayCallbacks,
                    inAppMessage.getDelay());
          }
        }
      }
    }
  }
}

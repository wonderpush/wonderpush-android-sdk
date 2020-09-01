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

package com.wonderpush.sdk.inappmessaging.display;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.squareup.picasso.Callback;
import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.NotificationManager;
import com.wonderpush.sdk.WonderPushCompatibilityHelper;
import com.wonderpush.sdk.inappmessaging.InAppMessaging;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks.InAppMessagingDismissType;
import com.wonderpush.sdk.inappmessaging.display.internal.BindingWrapperFactory;
import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import com.wonderpush.sdk.inappmessaging.display.internal.IamImageLoader;
import com.wonderpush.sdk.inappmessaging.display.internal.IamWindowManager;
import com.wonderpush.sdk.inappmessaging.display.internal.InAppMessageLayoutConfig;
import com.wonderpush.sdk.inappmessaging.display.internal.InAppMessagingDisplayImpl;
import com.wonderpush.sdk.inappmessaging.display.internal.Logging;
import com.wonderpush.sdk.inappmessaging.display.internal.RenewableTimer;
import com.wonderpush.sdk.inappmessaging.display.internal.bindingwrappers.BindingWrapper;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.components.AppComponent;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.components.DaggerAppComponent;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.components.DaggerUniversalComponent;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.components.UniversalComponent;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.modules.ApplicationModule;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.modules.HeadlessInAppMessagingModule;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.modules.InflaterConfigModule;
import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessagingScope;
import com.wonderpush.sdk.inappmessaging.model.BannerMessage;
import com.wonderpush.sdk.inappmessaging.model.CardMessage;
import com.wonderpush.sdk.inappmessaging.model.ImageData;
import com.wonderpush.sdk.inappmessaging.model.ImageOnlyMessage;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;
import com.wonderpush.sdk.inappmessaging.model.MessageType;
import com.wonderpush.sdk.inappmessaging.model.ModalMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import static com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator.Position.BOTTOM;
import static com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator.Position.TOP;

/**
 * The entry point of the In App Messaging display SDK.
 *
 * <p>In-App Messaging Display will automatically initialize, start listening for events,
 * and display eligible in-app messages.
 *
 */
@Keep
@InAppMessagingScope
public class InAppMessagingDisplay extends InAppMessagingDisplayImpl {
  static final long IMPRESSION_THRESHOLD_MILLIS = 1 * 1000; // 1 second is a valid impression
  static final long DISMISS_THRESHOLD_MILLIS =
      20 * 1000; // auto dismiss after 20 seconds for banner
  static final long INTERVAL_MILLIS = 1000;

  private static InAppMessagingDisplay instance;

  private final InAppMessaging headlessInAppMessaging;

  private final Map<String, Provider<InAppMessageLayoutConfig>> layoutConfigs;
  private final IamImageLoader imageLoader;
  private final RenewableTimer impressionTimer;
  private final RenewableTimer autoDismissTimer;
  private final IamWindowManager windowManager;
  private final BindingWrapperFactory bindingWrapperFactory;
  private final Application application;
  private final IamAnimator animator;

  private IamListener iamListener;
  private InAppMessage inAppMessage;
  private BindingWrapper bindingWrapper;
  private InAppMessagingDisplayCallbacks callbacks;
  private com.wonderpush.sdk.inappmessaging.InAppMessagingDisplay inAppMessagingDisplay;

  public @Nullable com.wonderpush.sdk.inappmessaging.InAppMessagingDisplay getDefaultInAppMessagingDisplay() {
    return inAppMessagingDisplay;
  }

  @Inject
  InAppMessagingDisplay(
      InAppMessaging headlessInAppMessaging,
      Map<String, Provider<InAppMessageLayoutConfig>> layoutConfigs,
      IamImageLoader imageLoader,
      RenewableTimer impressionTimer,
      RenewableTimer autoDismissTimer,
      IamWindowManager windowManager,
      Application application,
      BindingWrapperFactory bindingWrapperFactory,
      IamAnimator animator) {
    super();
    this.headlessInAppMessaging = headlessInAppMessaging;
    this.layoutConfigs = layoutConfigs;
    this.imageLoader = imageLoader;
    this.impressionTimer = impressionTimer;
    this.autoDismissTimer = autoDismissTimer;
    this.windowManager = windowManager;
    this.application = application;
    this.bindingWrapperFactory = bindingWrapperFactory;
    this.animator = animator;
  }

  /**
   * Get InAppMessagingDisplay instance
   */
  @NonNull
  @Keep
  public static void initialize(Application application,
                                                  InAppMessaging inAppMessaging) {
    if (instance == null) {
      UniversalComponent universalComponent =
              DaggerUniversalComponent.builder()
                      .applicationModule(new ApplicationModule(application))
                      .build();
      AppComponent appComponent =
              DaggerAppComponent.builder()
                      .universalComponent(universalComponent)
                      .headlessInAppMessagingModule(new HeadlessInAppMessagingModule(inAppMessaging))
                      .build();
      instance = appComponent.providesInAppMessagingUI();
      application.registerActivityLifecycleCallbacks(instance);
    }
  }

  public static @Nullable InAppMessagingDisplay getInstance() {
    return instance;
  }

  private static int getScreenOrientation(Application app) {
    return app.getResources().getConfiguration().orientation;
  }

  /**
   * Method that can be used to test the appearance of an in app message
   *
   * @hide
   */
  @Keep
  public void testMessage(
      Activity activity,
      InAppMessage inAppMessage,
      InAppMessagingDisplayCallbacks callbacks) {
    this.inAppMessage = inAppMessage;
    this.callbacks = callbacks;
    showActiveIam(activity);
  }

  /**
   * Sets iam listener to receive in app message callbacks
   *
   * @hide
   */
  @Keep
  public void setIamListener(IamListener listener) {
    this.iamListener = listener;
  }

  /**
   * Clears iam listener
   *
   * @hide
   */
  @Keep
  public void clearIamListener() {
    this.iamListener = null;
  }

  /**
   * Clears iam listener
   *
   * @hide
   */
  @Keep
  @Override
  public void onActivityStarted(final Activity activity) {
    super.onActivityStarted(activity);

    // Register IAM listener with the headless sdk.
    this.inAppMessagingDisplay = (iam, cb, delay) -> {
      // When we are in the middle of showing a message, we ignore other notifications these
      // messages will be fired when the corresponding events happen the next time.
      if (inAppMessage != null || headlessInAppMessaging.areMessagesSuppressed()) {
        Logging.logd("Active IAM exists. Skipping trigger");
        return true;
      }
      inAppMessage = iam;
      callbacks = cb;

      if (delay > 0) {
        new android.os.Handler().postDelayed(
                new Runnable() {
                  public void run() {
                    showActiveIam(activity);
                  }
                },
                delay);
      } else {
        showActiveIam(activity);
      }
      return true;
    };

    if (headlessInAppMessaging.getMessageDisplayComponent() == null) {
      headlessInAppMessaging.setMessageDisplayComponent(this.inAppMessagingDisplay);
    }

  }

  /**
   * Clear iam listener on activity paused
   *
   * @hide
   */
  @Keep
  @Override
  public void onActivityPaused(Activity activity) {
    // clear all state scoped to activity and dismiss iam
    if (this.inAppMessagingDisplay == headlessInAppMessaging.getMessageDisplayComponent()) {
      headlessInAppMessaging.clearDisplayListener();
    }
    imageLoader.cancelTag(activity.getClass());
    removeDisplayedIam(activity);
    super.onActivityPaused(activity);
  }

  /**
   * Clear iam listener on activity destroyed
   *
   * @hide
   */
  @Keep
  @Override
  public void onActivityDestroyed(Activity activity) {
    // clear all state scoped to activity and dismiss iam
    if (this.inAppMessagingDisplay == headlessInAppMessaging.getMessageDisplayComponent()) {
      headlessInAppMessaging.clearDisplayListener();
    }
    imageLoader.cancelTag(activity.getClass());
    removeDisplayedIam(activity);
    super.onActivityDestroyed(activity);
  }

  /**
   * Clear iam listener on activity resumed
   *
   * @hide
   */
  @Keep
  @Override
  public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    if (inAppMessage != null) {
      showActiveIam(activity);
    }
  }

  // The current IAM might be null
  @VisibleForTesting
  InAppMessage getCurrentInAppMessage() {
    return inAppMessage;
  }

  private void showActiveIam(@NonNull final Activity activity) {
    if (inAppMessage == null || headlessInAppMessaging.areMessagesSuppressed()) {
      Logging.loge("No active message found to render");
      return;
    }

    if (inAppMessage.getMessageType().equals(MessageType.UNSUPPORTED)) {
      Logging.loge("The message being triggered is not supported by this version of the sdk.");
      return;
    }
    notifyIamTrigger();

    InAppMessageLayoutConfig config =
        layoutConfigs
            .get(
                InflaterConfigModule.configFor(
                    inAppMessage.getMessageType(), getScreenOrientation(application)))
            .get();

    switch (inAppMessage.getMessageType()) {
      case BANNER:
        bindingWrapper = bindingWrapperFactory.createBannerBindingWrapper(config, inAppMessage);
        break;
      case MODAL:
        bindingWrapper = bindingWrapperFactory.createModalBindingWrapper(config, inAppMessage);
        break;
      case IMAGE_ONLY:
        bindingWrapper = bindingWrapperFactory.createImageBindingWrapper(config, inAppMessage);
        break;
      case CARD:
        bindingWrapper = bindingWrapperFactory.createCardBindingWrapper(config, inAppMessage);
        break;
      default:
        Logging.loge("No bindings found for this message type");
        // so we should break out completely and not attempt to show anything
        return;
    }

    // The WindowManager LayoutParams.TYPE_APPLICATION_PANEL requires tokens from the activity
    // which does not become available until after all lifecycle methods are complete.
    activity
        .findViewById(android.R.id.content)
        .post(
            new Runnable() {
              @Override
              public void run() {
                inflateBinding(activity);
              }
            });
  }

  // Since we handle only touch outside events and let the underlying views handle all other events,
  // it is safe to ignore this warning
  @SuppressLint("ClickableViewAccessibility")
  private void inflateBinding(final Activity activity) {
    if (bindingWrapper == null) return;

    // On click listener when X button or collapse button is clicked
    final View.OnClickListener dismissListener =
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (callbacks != null) {
              callbacks.messageDismissed(InAppMessagingDismissType.CLICK);
            }
            dismissIam(activity);
          }
        };

    List<View.OnClickListener> actionListeners = new ArrayList<>();
    // If the message has an action, but not an action url, we dismiss when the action
    // button is
    // clicked;
    for (List<ActionModel> actions : extractActions(inAppMessage)) {

      final View.OnClickListener actionListener;

      if (actions.size() > 0) {
        actionListener =
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                if (callbacks != null) {
                  callbacks.messageClicked(actions);
                }

                NotificationManager.handleActions(activity, inAppMessage.getNotificationMetadata(), actions);

                notifyIamClick();
                // Ensure that we remove the displayed IAM, and ensure that on re-load, the message
                // isn't re-displayed
                if (bindingWrapper != null && bindingWrapper.getExitAnimation() != null) {
                  animator.executeExitAnimation(bindingWrapper.getExitAnimation(), application, bindingWrapper.getRootView(), new IamAnimator.AnimationCompleteListener() {
                    @Override
                    public void onComplete() {
                      removeDisplayedIam(activity);
                    }
                  });
                } else {
                  removeDisplayedIam(activity);
                }
                inAppMessage = null;
                callbacks = null;
                bindingWrapper = null;
              }
            };
      } else {
        Logging.loge("No action url found for action.");
        actionListener = dismissListener;
      }
      actionListeners.add(actionListener);
    }

    final OnGlobalLayoutListener layoutListener =
        bindingWrapper.inflate(actionListeners, dismissListener);
    if (layoutListener != null) {
      bindingWrapper.getImageView().getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    // Show iam after image successfully loads
    loadNullableImage(
        activity,
        bindingWrapper,
        extractImageData(inAppMessage),
        new Callback() {
          @Override
          public void onSuccess() {
            // Setup dismiss on touch outside
            if (!bindingWrapper.getConfig().backgroundEnabled()) {
              bindingWrapper
                  .getRootView()
                  .setOnTouchListener(
                      new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                          if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                            if (callbacks != null) {
                              callbacks.messageDismissed(
                                  InAppMessagingDismissType.UNKNOWN_DISMISS_TYPE);
                            }
                            dismissIam(activity);
                            return true;
                          }
                          return false;
                        }
                      });
            }

            // Setup impression timer
            impressionTimer.start(
                new RenewableTimer.Callback() {
                  @Override
                  public void onFinish() {
                    if (inAppMessage != null && callbacks != null) {
                      Logging.logi(
                          "Impression timer onFinish for: "
                              + inAppMessage.getNotificationMetadata().getCampaignId());

                      callbacks.impressionDetected();
                    }
                  }
                },
                IMPRESSION_THRESHOLD_MILLIS,
                INTERVAL_MILLIS);

            // Setup auto dismiss timer
            if (bindingWrapper.getConfig().autoDismiss()) {
              autoDismissTimer.start(
                  new RenewableTimer.Callback() {
                    @Override
                    public void onFinish() {
                      if (inAppMessage != null && callbacks != null) {
                        callbacks.messageDismissed(InAppMessagingDismissType.AUTO);
                      }

                      dismissIam(activity);
                    }
                  },
                  DISMISS_THRESHOLD_MILLIS,
                  INTERVAL_MILLIS);
            }

            activity.runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    windowManager.show(bindingWrapper, activity);
                    if (bindingWrapper.getEntryAnimation() != null) {
                      animator.executeEntryAnimation(bindingWrapper.getEntryAnimation(), application, bindingWrapper.getRootView(), null);
                    }
                  }
                });
          }

          @Override
          public void onError(Exception e) {
            Logging.loge("Image download failure ");
            if (layoutListener != null) {
              WonderPushCompatibilityHelper.removeOnGlobalLayoutListener(
                  bindingWrapper
                      .getImageView()
                      .getViewTreeObserver(),
                  layoutListener
              );
            }
            cancelTimers(); // Not strictly necessary.
            inAppMessage = null;
            callbacks = null;
            bindingWrapper = null;
          }
        });
  }

  /**
   * Returns a list of list of actions. The first list of list corresponds to primary actions, the second to secondary actions if any.
   * @param message
   * @return
   */
  private List<List<ActionModel>> extractActions(InAppMessage message) {
    List<List<ActionModel>> result = new ArrayList<>();
    switch (message.getMessageType()) {
      case BANNER:
        result.add(((BannerMessage) message).getActions());
        break;
      case CARD:
        result.add(((CardMessage) message).getPrimaryActions());
        result.add(((CardMessage) message).getSecondaryActions());
        break;
      case IMAGE_ONLY:
        result.add(((ImageOnlyMessage) message).getActions());
        break;
      case MODAL:
        result.add(((ModalMessage) message).getActions());
        break;
      default:
        // An empty action is treated like a dismiss
        result.add(Collections.emptyList());
    }
    return result;
  }

  // TODO: Factor this into the InAppMessage API.
  private ImageData extractImageData(InAppMessage message) {
    // Handle getting image data for card type
    if (message.getMessageType() == MessageType.CARD) {
      ImageData portraitImageData = ((CardMessage) message).getPortraitImageData();
      ImageData landscapeImageData = ((CardMessage) message).getLandscapeImageData();

      // If we're in portrait try to use portrait image data, fallback to landscape
      if (getScreenOrientation(application) == Configuration.ORIENTATION_PORTRAIT) {
        return isValidImageData(portraitImageData) ? portraitImageData : landscapeImageData;
      }
      // If we're in landscape try to use landscape image data, fallback to portrait
      return isValidImageData(landscapeImageData) ? landscapeImageData : portraitImageData;
    }
    // For now this is how we get all other iam types image data.
    if (message instanceof InAppMessage.InAppMessageWithImage) {
      return ((InAppMessage.InAppMessageWithImage)message).getImageData();
    }
    return null;
  }

  // TODO: Factor this into the InAppMessage API
  private boolean isValidImageData(@Nullable ImageData imageData) {
    return imageData != null && !TextUtils.isEmpty(imageData.getImageUrl());
  }

  private void loadNullableImage(
          Activity activity, BindingWrapper iam, ImageData imageData, Callback callback) {
    if (isValidImageData(imageData)) {
      imageLoader
          .load(imageData.getImageUrl())
          .tag(activity.getClass())
          .into(iam.getImageView(), callback);
    } else {
      callback.onSuccess();
    }
  }

  // This action needs to be idempotent since multiple callbacks compete to dismiss.
  // For example, a swipe and a click on the banner compete.
  private void dismissIam(Activity activity) {
    Logging.logd("Dismissing iam");
    notifyIamDismiss();
    if (bindingWrapper != null && bindingWrapper.getExitAnimation() != null) {
      animator.executeExitAnimation(bindingWrapper.getExitAnimation(), application, bindingWrapper.getRootView(), new IamAnimator.AnimationCompleteListener() {
        @Override
        public void onComplete() {
          removeDisplayedIam(activity);
        }
      });
    } else {
      removeDisplayedIam(activity);
    }
    inAppMessage = null;
    callbacks = null;
    bindingWrapper = null;
  }

  private void removeDisplayedIam(Activity activity) {
    if (windowManager.isIamDisplayed()) {
      windowManager.destroy(activity);
      cancelTimers();
    }
  }

  private void cancelTimers() {
    impressionTimer.cancel();
    autoDismissTimer.cancel();
  }

  private void notifyIamTrigger() {
    if (iamListener != null) {
      iamListener.onIamTrigger();
    }
  }

  private void notifyIamClick() {
    if (iamListener != null) {
      iamListener.onIamClick();
    }
  }

  private void notifyIamDismiss() {
    if (iamListener != null) {
      iamListener.onIamDismiss();
    }
  }
}

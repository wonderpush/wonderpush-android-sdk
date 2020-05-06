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

import android.support.annotation.NonNull;

import com.wonderpush.sdk.ActionModel;
import com.wonderpush.sdk.inappmessaging.InAppMessagingClickListener;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayCallbacks;
import com.wonderpush.sdk.inappmessaging.InAppMessagingDisplayErrorListener;
import com.wonderpush.sdk.inappmessaging.InAppMessagingImpressionListener;
import com.wonderpush.sdk.inappmessaging.model.InAppMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class used to manage and schedule events to registered (ie: developer-defined) or expensive
 * listeners
 *
 * @hide
 */
@SuppressWarnings("JavaDoc")
public class DeveloperListenerManager {

  // We limit to 1 so there is minimial impact to device performance
  private static final int POOL_SIZE = 1;
  // Keep alive to minimize chance of having to restart a thread to handle both impression and click
  private static final int KEEP_ALIVE_TIME_SECONDS = 15;
  public static DeveloperListenerManager instance = new DeveloperListenerManager();
  private Map<InAppMessagingClickListener, ClicksExecutorAndListener>
      registeredClickListeners = new HashMap<>();
  private Map<InAppMessagingDisplayErrorListener, ErrorsExecutorAndListener>
      registeredErrorListeners = new HashMap<>();;
  private Map<InAppMessagingImpressionListener, ImpressionExecutorAndListener>
      registeredImpressionListeners = new HashMap<>();;

  private static BlockingQueue<Runnable> mCallbackQueue = new LinkedBlockingQueue<>();
  private static final ThreadPoolExecutor CALLBACK_QUEUE_EXECUTOR =
      new ThreadPoolExecutor(
          POOL_SIZE,
          POOL_SIZE,
          KEEP_ALIVE_TIME_SECONDS,
          TimeUnit.SECONDS,
          mCallbackQueue,
          new IAMThreadFactory("EventListeners-"));

  static {
    CALLBACK_QUEUE_EXECUTOR.allowCoreThreadTimeOut(true);
  }

  // Used internally by MetricsLoggerClient
  public void impressionDetected(InAppMessage inAppMessage) {
    for (ImpressionExecutorAndListener listener : registeredImpressionListeners.values()) {
      listener
          .withExecutor(CALLBACK_QUEUE_EXECUTOR)
          .execute(() -> listener.getListener().impressionDetected(inAppMessage));
    }
  }

  public void displayErrorEncountered(
      InAppMessage inAppMessage,
      InAppMessagingDisplayCallbacks.InAppMessagingErrorReason errorReason) {
    for (ErrorsExecutorAndListener listener : registeredErrorListeners.values()) {
      listener
          .withExecutor(CALLBACK_QUEUE_EXECUTOR)
          .execute(() -> listener.getListener().displayErrorEncountered(inAppMessage, errorReason));
    }
  }

  public void messageClicked(InAppMessage inAppMessage, List<ActionModel> actions) {
    for (ClicksExecutorAndListener listener : registeredClickListeners.values()) {
      listener
          .withExecutor(CALLBACK_QUEUE_EXECUTOR)
          .execute(() -> listener.getListener().messageClicked(inAppMessage, actions));
    }
  }

  // pass through from InAppMessaging public api
  public void addImpressionListener(InAppMessagingImpressionListener impressionListener) {
    registeredImpressionListeners.put(
        impressionListener, new ImpressionExecutorAndListener(impressionListener));
  }

  public void addClickListener(InAppMessagingClickListener clickListener) {
    registeredClickListeners.put(clickListener, new ClicksExecutorAndListener(clickListener));
  }

  public void addDisplayErrorListener(
      InAppMessagingDisplayErrorListener displayErrorListener) {
    registeredErrorListeners.put(
        displayErrorListener, new ErrorsExecutorAndListener(displayErrorListener));
  }

  // Executed with provided executor
  public void addImpressionListener(
          InAppMessagingImpressionListener impressionListener, Executor executor) {
    registeredImpressionListeners.put(
        impressionListener, new ImpressionExecutorAndListener(impressionListener, executor));
  }

  public void addClickListener(
          InAppMessagingClickListener clickListener, Executor executor) {
    registeredClickListeners.put(
        clickListener, new ClicksExecutorAndListener(clickListener, executor));
  }

  public void addDisplayErrorListener(
          InAppMessagingDisplayErrorListener displayErrorListener, Executor executor) {
    registeredErrorListeners.put(
        displayErrorListener, new ErrorsExecutorAndListener(displayErrorListener, executor));
  }

  // Removing individual listeners:
  public void removeImpressionListener(
      InAppMessagingImpressionListener impressionListener) {
    registeredImpressionListeners.remove(impressionListener);
  }

  public void removeClickListener(InAppMessagingClickListener clickListener) {
    registeredClickListeners.remove(clickListener);
  }

  public void removeDisplayErrorListener(
      InAppMessagingDisplayErrorListener displayErrorListener) {
    registeredErrorListeners.remove(displayErrorListener);
  }

  /** The thread factory for Storage threads. */
  static class IAMThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String mNameSuffix;

    IAMThreadFactory(@NonNull String suffix) {
      mNameSuffix = suffix;
    }

    @SuppressWarnings("ThreadPriorityCheck")
    @Override
    public Thread newThread(@NonNull Runnable r) {
      Thread t = new Thread(r, "IAM-" + mNameSuffix + threadNumber.getAndIncrement());
      t.setDaemon(false);
      t.setPriority(
          android.os.Process.THREAD_PRIORITY_BACKGROUND
              + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
      return t;
    }
  }

  private abstract static class ExecutorAndListener<T> {

    private final Executor executor;

    public abstract T getListener();

    public Executor withExecutor(Executor defaultExecutor) {
      if (executor == null) {
        return defaultExecutor;
      }
      return executor;
    }

    public ExecutorAndListener(Executor e) {
      this.executor = e;
    }
  }

  private static class ImpressionExecutorAndListener
      extends ExecutorAndListener<InAppMessagingImpressionListener> {
    InAppMessagingImpressionListener listener;

    public ImpressionExecutorAndListener(
            InAppMessagingImpressionListener listener, Executor e) {
      super(e);
      this.listener = listener;
    }

    public ImpressionExecutorAndListener(InAppMessagingImpressionListener listener) {
      super(null);
      this.listener = listener;
    }

    @Override
    public InAppMessagingImpressionListener getListener() {
      return listener;
    }
  }

  private static class ClicksExecutorAndListener
      extends ExecutorAndListener<InAppMessagingClickListener> {
    InAppMessagingClickListener listener;

    public ClicksExecutorAndListener(InAppMessagingClickListener listener, Executor e) {
      super(e);
      this.listener = listener;
    }

    public ClicksExecutorAndListener(InAppMessagingClickListener listener) {
      super(null);
      this.listener = listener;
    }

    @Override
    public InAppMessagingClickListener getListener() {
      return listener;
    }
  }

  private static class ErrorsExecutorAndListener
      extends ExecutorAndListener<InAppMessagingDisplayErrorListener> {
    InAppMessagingDisplayErrorListener listener;

    public ErrorsExecutorAndListener(
            InAppMessagingDisplayErrorListener listener, Executor e) {
      super(e);
      this.listener = listener;
    }

    public ErrorsExecutorAndListener(InAppMessagingDisplayErrorListener listener) {
      super(null);
      this.listener = listener;
    }

    @Override
    public InAppMessagingDisplayErrorListener getListener() {
      return listener;
    }
  }
}

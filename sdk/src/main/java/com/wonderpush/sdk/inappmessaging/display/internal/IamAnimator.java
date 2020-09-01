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

package com.wonderpush.sdk.inappmessaging.display.internal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Application;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.wonderpush.sdk.inappmessaging.display.internal.injection.scopes.InAppMessagingScope;

import javax.inject.Inject;

/** @hide */
@InAppMessagingScope
public class IamAnimator {

  public enum EntryAnimation {
    FADE_IN("fadeIn"),
    SLIDE_IN_FROM_RIGHT("slideInFromRight"),
    SLIDE_IN_FROM_LEFT("slideInFromLeft"),
    SLIDE_IN_FROM_TOP("slideInFromTop"),
    SLIDE_IN_FROM_BOTTOM("slideInFromBottom");
    public final String slug;
    EntryAnimation(String slug) {
      this.slug = slug;
    }
    public static @NonNull EntryAnimation fromSlug(String s) {
      for (EntryAnimation a : EntryAnimation.values()) {
        if (a.slug.equals(s)) return a;
      }
      return FADE_IN;
    }
  }
  public enum ExitAnimation {
    FADE_OUT("fadeOut"),
    SLIDE_OUT_RIGHT("slideOutRight"),
    SLIDE_OUT_LEFT("slideOutLeft"),
    SLIDE_OUT_TOP("slideOutUp"),
    SLIDE_OUT_DOWN("slideOutDown");
    public final String slug;
    ExitAnimation(String slug) {
      this.slug = slug;
    }
    public static @NonNull ExitAnimation fromSlug(String s) {
      for (ExitAnimation a : ExitAnimation.values()) {
        if (a.slug.equals(s)) return a;
      }
      return FADE_OUT;
    }
  }

  @Inject
  IamAnimator() {}

  public void executeExitAnimation(ExitAnimation animation, final Application app, final View view, @Nullable final AnimationCompleteListener completeListener) {
    switch (animation) {
      case SLIDE_OUT_DOWN:
        slideOutOfView(app, view, Position.BOTTOM, completeListener);
        break;
      case SLIDE_OUT_TOP:
        slideOutOfView(app, view, Position.TOP, completeListener);
        break;
      case SLIDE_OUT_LEFT:
        slideOutOfView(app, view, Position.LEFT, completeListener);
        break;
      case SLIDE_OUT_RIGHT:
        slideOutOfView(app, view, Position.RIGHT, completeListener);
        break;
      case FADE_OUT:
        fadeOut(app, view, completeListener);
        break;
    }  }

  public void executeEntryAnimation(EntryAnimation animation, final Application app, final View view, @Nullable final AnimationCompleteListener completeListener) {
    switch (animation) {
      case SLIDE_IN_FROM_BOTTOM:
        slideIntoView(app, view, Position.BOTTOM ,completeListener);
        break;
      case SLIDE_IN_FROM_TOP:
        slideIntoView(app, view, Position.TOP, completeListener);
        break;
      case SLIDE_IN_FROM_LEFT:
        slideIntoView(app, view, Position.LEFT, completeListener);
        break;
      case SLIDE_IN_FROM_RIGHT:
        slideIntoView(app, view, Position.RIGHT, completeListener);
        break;
      case FADE_IN:
        fadeIn(app, view, completeListener);
        break;
    }
  }

  private void fadeIn(Application app, View view, AnimationCompleteListener completeListener) {
    view.setAlpha(0.0f);
    view.animate()
        .setDuration(app.getResources().getInteger(android.R.integer.config_shortAnimTime))
        .alpha(1.0f)
        .setListener(completeListener != null ? new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            completeListener.onComplete();
          }

          @Override
          public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            completeListener.onComplete();
          }
    } : null);
  }

  private void fadeOut(Application app, View view, AnimationCompleteListener completeListener) {
    view.setAlpha(1.0f);
    view.animate()
            .setDuration(app.getResources().getInteger(android.R.integer.config_shortAnimTime))
            .alpha(0.0f)
            .setListener(completeListener != null ? new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                completeListener.onComplete();
              }

              @Override
              public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                completeListener.onComplete();
              }
            } : null);
  }

  /**
   * This method currently assumes that the passed in view has {@link ViewGroup.LayoutParams} set to
   * WRAP_CONTENT
   */
  private void slideIntoView(final Application app, final View view, Position startPosition, @Nullable final AnimationCompleteListener completeListener) {
    view.setAlpha(0.0f);
    Point start = Position.getPoint(startPosition, view);

    view.animate()
        .translationX(start.x)
        .translationY(start.y)
        .setDuration(1)
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                view.animate()
                    .translationX(0)
                    .translationY(0)
                    .alpha(1.0f)
                    .setDuration(
                        app.getResources().getInteger(android.R.integer.config_longAnimTime))
                    .setListener(completeListener != null ?new AnimatorListenerAdapter() {
                      @Override
                      public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        completeListener.onComplete();
                      }
                      @Override
                      public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        completeListener.onComplete();
                      }
                    } : null);
              }
            });
  }

  private void slideOutOfView(
      final Application app,
      final View view,
      Position end,
      final AnimationCompleteListener completeListener) {
    Point start = Position.getPoint(end, view);

    AnimatorListenerAdapter animatorListenerAdapter =
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            // Remove iam from window only after the animation is complete
            completeListener.onComplete();
          }
        };

    view.animate()
        .translationX(start.x)
        .translationY(start.y)
        .setDuration(app.getResources().getInteger(android.R.integer.config_longAnimTime))
        .setListener(animatorListenerAdapter);
  }

  public enum Position {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM;

    private static Point getPoint(Position d, View view) {
      view.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

      switch (d) {
        case LEFT:
          return new Point(-1 * view.getMeasuredWidth(), 0);
        case RIGHT:
          return new Point(1 * view.getMeasuredWidth(), 0);
        case TOP:
          return new Point(0, -1 * view.getMeasuredHeight());
        case BOTTOM:
          return new Point(0, 1 * view.getMeasuredHeight());
        default:
          return new Point(0, -1 * view.getMeasuredHeight());
      }
    }
  }

  public interface AnimationCompleteListener {
    void onComplete();
  }
}

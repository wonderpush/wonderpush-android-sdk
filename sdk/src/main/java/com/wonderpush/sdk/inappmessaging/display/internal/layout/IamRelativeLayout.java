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

package com.wonderpush.sdk.inappmessaging.display.internal.layout;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.MotionEvent;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.wonderpush.sdk.R;
import com.wonderpush.sdk.inappmessaging.display.internal.IamAnimator;
import com.wonderpush.sdk.inappmessaging.display.internal.layout.util.BackButtonHandler;

import java.lang.ref.WeakReference;

/** @hide */
public class IamRelativeLayout extends RelativeLayout implements BackButtonLayout, IamAnimator.DisableTouchLayout {

  private BackButtonHandler mBackHandler;
  private boolean mTouchDisabled;
  private boolean mForwardTouchEventsToApplicationWindow;

  public IamRelativeLayout(Context context) {
    super(context);
  }

  public IamRelativeLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IamRelativeLayout, 0, 0);
    try {
      mForwardTouchEventsToApplicationWindow = a.getBoolean(R.styleable.IamRelativeLayout_forwardTouchEventsToApplicationWindow, false);
      setClickable(!mForwardTouchEventsToApplicationWindow);
    } finally {
      a.recycle();
    }

  }

  public IamRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @RequiresApi(21)
  public IamRelativeLayout(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  public void setDismissListener(OnClickListener listener) {
    mBackHandler = new BackButtonHandler(this, listener);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    Boolean handled = mBackHandler != null ? mBackHandler.dispatchKeyEvent(event) : null;
    if (handled != null) {
      return handled;
    } else {
      return super.dispatchKeyEvent(event);
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mTouchDisabled) {
      return true;
    }
    return  super.onInterceptTouchEvent(ev);
  }

  @Override
  public void setTouchDisabled(boolean disabled) {
    mTouchDisabled = disabled;
    setClickable(!disabled);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mForwardTouchEventsToApplicationWindow) {
      if (mApplicationWindow != null && mApplicationWindow.get() != null) {
        mApplicationWindow.get().dispatchTouchEvent(event);
      }
      return false;
    }
    return super.onTouchEvent(event);
  }

  private WeakReference<View> mApplicationWindow;
  public void setActivity(Activity activity) {
    View applicationWindow = activity.findViewById(android.R.id.content);
    while (applicationWindow != null && applicationWindow.getParent() instanceof View) {
      applicationWindow = (View)applicationWindow.getParent();
    }
    if (applicationWindow != null) {
      mApplicationWindow = new WeakReference<>(applicationWindow);
    }
  }
}

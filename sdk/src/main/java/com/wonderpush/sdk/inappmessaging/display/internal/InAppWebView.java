package com.wonderpush.sdk.inappmessaging.display.internal;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InAppWebView extends WebView {
    private Rect clipPath;
    public InAppWebView(@NonNull Context context) {
        super(context);
    }

    public InAppWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InAppWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public InAppWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public Rect getClipPath() {
        return clipPath;
    }

    public void setClipPath(Rect clipPath) {
        this.clipPath = clipPath;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        for (int i = 0; this.clipPath != null && i < event.getPointerCount(); i++) {
            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            event.getPointerCoords(i, coords);
            int x = (int)(coords.x / metrics.density);
            int y = (int)(coords.y / metrics.density);
            boolean pointInRect =
                    x >= this.clipPath.left
                    && x <= this.clipPath.right
                    && y >= this.clipPath.top
                    && y <= this.clipPath.bottom;

            // Ignore event if any pointer is outside the clipPath
            if (!pointInRect) {
                return false;
            }
        }
        return super.onTouchEvent(event);
    }
}

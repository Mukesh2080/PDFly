package com.mukesh.pdfly.pdfrenderer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ScrollView;

public class ZoomableScrollView extends ScrollView {
    private float scaleFactor = 1f;
    private ScaleGestureDetector scaleDetector;
    private View contentView;

    public ZoomableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Pass only if two fingers are used
        if (ev.getPointerCount() > 1) {
            scaleDetector.onTouchEvent(ev);
            return true; // Consume gesture
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            scaleDetector.onTouchEvent(ev);
            return true; // Block ScrollView default behavior during zoom
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        contentView = getChildAt(0);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(1f, Math.min(scaleFactor, 5f));

            if (contentView != null) {
                contentView.setScaleX(scaleFactor);
                contentView.setScaleY(scaleFactor);
                contentView.requestLayout(); // 🔥 This makes scroll boundaries adjust after zoom
            }
            return true;
        }
    }
}

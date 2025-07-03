package com.mukesh.pdfly.signature.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;

public abstract class OverlayElementView extends FrameLayout {

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float scaleFactor = 1f;
    private float rotationDegrees = 0f;
    private float lastX, lastY;
    private boolean isSelected = false;

    public OverlayElementView(Context context) {
        super(context);
        gestureDetector = new GestureDetector(context, new GestureListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        initCommon();
    }

    public OverlayElementView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetector(context, new GestureListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        initCommon();
    }

    public void initCommon() {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);

        setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (getContext() instanceof PdfEditorActivity) {
                    ((PdfEditorActivity) getContext()).onElementSelected(this);
                }
                lastX = event.getRawX();
                lastY = event.getRawY();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - lastX;
                float dy = event.getRawY() - lastY;

                setX(getX() + dx);
                setY(getY() + dy);

                lastX = event.getRawX();
                lastY = event.getRawY();
            }
            return true;
        });
    }

    public void applyTransformations() {
        setScaleX(scaleFactor);
        setScaleY(scaleFactor);
        setRotation(rotationDegrees);
    }

    public void setSelectedState(boolean selected) {
        isSelected = selected;
        //setBackgroundResource(selected ? R.drawable.bg_element_selected : 0);
    }

    public boolean isSelectedElement() {
        return isSelected;
    }

    public void rotateBy(float degrees) {
        rotationDegrees += degrees;
        applyTransformations();
    }

    public void scaleBy(float scale) {
        scaleFactor *= scale;
        applyTransformations();
    }

    public void setColor(int color) {
    }

    // Gesture Listeners
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleBy(detector.getScaleFactor());
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            rotateBy(15); // Optional: rotate on double-tap
            return true;
        }
    }
    public boolean hitTest(float globalX, float globalY) {
        int[] location = new int[2];
        getLocationOnScreen(location);
        Rect rect = new Rect(location[0], location[1],
                location[0] + getWidth(),
                location[1] + getHeight());
        return rect.contains((int) globalX, (int) globalY);
    }

}

package com.mukesh.pdfly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

//mukesh
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

public class ZoomableFrameLayout extends FrameLayout {

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;
    private static final float PAN_THRESHOLD_DP = 8; // Adjust this value as needed, in DP

    private ScaleGestureDetector scaleDetector;
    private Matrix scaleMatrix = new Matrix();
    private float[] matrixValues = new float[9];

    // These are for dragging
    private float lastTouchX, lastTouchY;
    private static final int INVALID_POINTER_ID = -1;
    private int activePointerId = INVALID_POINTER_ID;

    // Flag to indicate if the current gesture is being handled by ZoomableFrameLayout for pan/zoom
    private boolean isZoomOrPanActive = false;
    private float scaledPanThreshold; // Converted DP to pixels

    public ZoomableFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public ZoomableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        setWillNotDraw(false); // We need to manually control the transformations of child views

        // Calculate the pan threshold in pixels based on device density
        scaledPanThreshold = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                PAN_THRESHOLD_DP,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 1. If drawing mode is active, let the DrawView handle it entirely.
        //    DrawView's onTouchEvent should manage disallowInterceptTouchEvent.
        if (isDrawingMode) {
            return super.onTouchEvent(event); // Let children (DrawView) handle
        }

        // 2. Always pass events to ScaleGestureDetector first.
        boolean handledByScaleDetector = scaleDetector.onTouchEvent(event);

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Reset active state
                isZoomOrPanActive = false;
                // Capture initial touch points
                activePointerId = event.getPointerId(0);
                lastTouchX = event.getX(0);
                lastTouchY = event.getY(0);

                // If starting with multiple fingers, assume zoom and disallow parent.
                if (event.getPointerCount() > 1) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    isZoomOrPanActive = true; // Mark as active for zoom
                }
                // If starting with one finger and already zoomed in, we *might* pan.
                // We initially allow parent interception, and only disallow on ACTION_MOVE
                // if a significant pan gesture begins.
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (scaleDetector.isInProgress()) {
                    // Scale gesture is active, keep disallowing interception.
                    getParent().requestDisallowInterceptTouchEvent(true);
                    isZoomOrPanActive = true; // Ensure this flag is set during scaling
                } else if (event.getPointerCount() == 1 && getScale() > MIN_SCALE + 0.01f) { // Check if truly zoomed in
                    // Single pointer, and we are zoomed in, this is a potential pan.
                    final int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        final float currentX = event.getX(pointerIndex);
                        final float currentY = event.getY(pointerIndex);

                        final float dx = currentX - lastTouchX;
                        final float dy = currentY - lastTouchY;

                        // Calculate total distance moved since ACTION_DOWN or last reposition
                        float movedX = currentX - lastTouchX;
                        float movedY = currentY - lastTouchY;

                        // Only disallow interception and start panning if movement exceeds threshold
//                        if (!isZoomOrPanActive && (Math.abs(movedX) > scaledPanThreshold || Math.abs(movedY) > scaledPanThreshold)) {
//                            getParent().requestDisallowInterceptTouchEvent(true);
//                            isZoomOrPanActive = true; // Now we are actively panning
//                        }
                        if (getScale() > MIN_SCALE + 0.01f) {
                            getParent().requestDisallowInterceptTouchEvent(true); // 🔥 Stronger disallow early
                        }

                        if (!isZoomOrPanActive && (Math.abs(movedX) > scaledPanThreshold || Math.abs(movedY) > scaledPanThreshold)) {
                            isZoomOrPanActive = true;
                        }

                        if (isZoomOrPanActive) {
                            scaleMatrix.postTranslate(dx, dy);
                            fixTranslation();
                            invalidate();

                            lastTouchX = currentX; // Update last touch for next move
                            lastTouchY = currentY;
                        }
                    }
                } else {
                    // Not zooming (scaleDetector not in progress), not panning a zoomed view,
                    // or scale is MIN_SCALE (or very close), so let parent ScrollView handle.
                    getParent().requestDisallowInterceptTouchEvent(false);
                    isZoomOrPanActive = false; // Reset if not actively handling
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = INVALID_POINTER_ID;
                // Always allow parent to intercept touch events again when gesture ends.
                getParent().requestDisallowInterceptTouchEvent(false);
                isZoomOrPanActive = false; // Reset active state
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIndex);

                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                // If after a POINTER_UP, only one pointer remains, and the view is zoomed,
                // we should assume the user might now try to pan with that single finger.
                // We don't necessarily call requestDisallowInterceptTouchEvent(true) here
                // because the next ACTION_MOVE will determine if a pan starts.
                break;
            }
        }

        // Final decision on consuming the event:
        // We consume if the scale detector handled it OR if we are currently
        // managing a zoom/pan gesture.
        // Note: We don't return true just because getScale() > MIN_SCALE, as this
        // could prevent the ScrollView from ever getting events if a page is slightly
        // zoomed and the user wants to scroll past it without panning it.
        return handledByScaleDetector || isZoomOrPanActive;
    }

    // ... (rest of your ZoomableFrameLayout code - dispatchDraw, getScale, fixTranslation, ScaleListener, getTransformationMatrix, setDrawingMode)
    // The ScaleListener should remain largely the same, ensuring it sets isZoomOrPanActive=true and requests disallow.
    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.concat(scaleMatrix);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private float getScale() {
        scaleMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    private void fixTranslation() {
        scaleMatrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scale = getScale();

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float contentWidth = viewWidth * scale;
        float contentHeight = viewHeight * scale;

        float minTransX = viewWidth - contentWidth;
        float minTransY = viewHeight - contentHeight;

        // Clamp translation within bounds
        transX = Math.max(minTransX, Math.min(transX, 0));
        transY = Math.max(minTransY, Math.min(transY, 0));

        // If content is smaller than the view, center it.
        if (contentWidth < viewWidth) {
            transX = (viewWidth - contentWidth) / 2;
        }
        if (contentHeight < viewHeight) {
            transY = (viewHeight - contentHeight) / 2;
        }

        matrixValues[Matrix.MTRANS_X] = transX;
        matrixValues[Matrix.MTRANS_Y] = transY;
        scaleMatrix.setValues(matrixValues);
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (isDrawingMode) {
                return false;
            }

            float scaleFactor = detector.getScaleFactor();
            float currentScale = getScale();

            float newScale = currentScale * scaleFactor;

            // Clamp the new scale
            if (newScale > MAX_SCALE) {
                scaleFactor = MAX_SCALE / currentScale;
            } else if (newScale < MIN_SCALE) {
                scaleFactor = MIN_SCALE / currentScale;
            }

            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor)) {
                return false;
            }

            scaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            fixTranslation();
            invalidate();

            isZoomOrPanActive = true; // We are actively zooming
            getParent().requestDisallowInterceptTouchEvent(true); // Ensure parent doesn't intercept during scale
            return true;
        }
    }
    public Matrix getTransformationMatrix() {
        return scaleMatrix;
    }
    private boolean isDrawingMode = false;

    // Add a public method to set the mode from your Activity
    public void setDrawingMode(boolean isDrawingMode) {
        this.isDrawingMode = isDrawingMode;
        // When drawing mode changes, ensure correct interception state for the next touch.
        // No immediate action here, it's handled by onTouchEvent's initial check.
    }
}
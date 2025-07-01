package com.mukesh.pdfly.pdfrenderer.views;

import android.content.Context;
import android.graphics.Matrix;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;

//
public class ZoomLayout extends FrameLayout {

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;

    private ScaleGestureDetector scaleDetector;
    private Matrix scaleMatrix = new Matrix();
    private float[] matrixValues = new float[9];

    // These are for dragging
    private float lastTouchX, lastTouchY;
    private static final int INVALID_POINTER_ID = -1;
    private int activePointerId = INVALID_POINTER_ID;

    // Flag to indicate if the current gesture is being handled by ZoomableFrameLayout
    private boolean isZoomOrPanActive = false;

    public ZoomLayout(Context context) {
        super(context);
        init(context);
    }

    public ZoomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        setWillNotDraw(false); // We need to manually control the transformations of child views
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If drawing mode is active, let the DrawView handle it.
        // The DrawView's onTouchEvent will return true and disallow interception.
        if (isDrawingMode) {
            // No need to call scaleDetector.onTouchEvent(event) or handle pan here,
            // as the DrawView will take over.
            return super.onTouchEvent(event); // Let children (DrawView) handle if drawingMode is active
            // DrawView itself will manage disallowInterceptTouchEvent.
        }

        // Pass all events to the ScaleGestureDetector first.
        boolean handledByScaleDetector = scaleDetector.onTouchEvent(event);

        final int action = event.getActionMasked();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getContext() instanceof PdfEditorActivity) {
                ((PdfEditorActivity) getContext()).deselectAllOverlays();
            }
            return super.onTouchEvent(event);
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // If the scale detector has consumed the event (e.g., two fingers down),
                // or if we decide to handle it for single-finger pan
                if (event.getPointerCount() == 1) { // Only consider potential pan with one pointer
                    final int pointerIndex = event.getActionIndex();
                    lastTouchX = event.getX(pointerIndex);
                    lastTouchY = event.getY(pointerIndex);
                    activePointerId = event.getPointerId(0);
                    // Decide if we might pan. Initially assume no pan, let parent scroll.
                    // We'll disallow interception on ACTION_MOVE if actual panning occurs.
                    isZoomOrPanActive = false;
                } else if (event.getPointerCount() > 1) {
                    // Two or more fingers are down, likely a zoom gesture.
                    // Disallow parent interception immediately for zoom.
                    getParent().requestDisallowInterceptTouchEvent(true);
                    isZoomOrPanActive = true;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (scaleDetector.isInProgress()) {
                    // Scale gesture is active, keep disallowing interception.
                    getParent().requestDisallowInterceptTouchEvent(true);
                    isZoomOrPanActive = true;
                } else if (event.getPointerCount() == 1 && getScale() > 1.0f) {
                    // Single pointer, and we are zoomed in, so this is a potential pan.
                    final int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        final float dx = event.getX(pointerIndex) - lastTouchX;
                        final float dy = event.getY(pointerIndex) - lastTouchY;

                        // Only disallow interception if there's significant movement (pan)
                        // This prevents small accidental movements from blocking scrolling.
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) { // Threshold for pan
                            getParent().requestDisallowInterceptTouchEvent(true);
                            isZoomOrPanActive = true;

                            scaleMatrix.postTranslate(dx, dy);
                            fixTranslation();
                            invalidate();

                            lastTouchX = event.getX(pointerIndex);
                            lastTouchY = event.getY(pointerIndex);
                        } else {
                            // Movement below threshold, let parent scroll (don't set disallow to true)
                            // However, if it was already disallowed, we need to consider if it should be allowed again.
                            // Best practice is to allow on UP/CANCEL.
                        }
                    }
                } else {
                    // Not zooming, not panning (either scale is 1.0f or multiple pointers but no scale gesture yet)
                    // or drawing mode is off, so allow parent to intercept for scrolling.
                    getParent().requestDisallowInterceptTouchEvent(false);
                    isZoomOrPanActive = false;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = INVALID_POINTER_ID;
                // Always allow parent to intercept touch events again when gesture ends.
                getParent().requestDisallowInterceptTouchEvent(false);
                isZoomOrPanActive = false;
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
                // If only one pointer remains after one goes up, and we are zoomed,
                // we might transition to pan. But for now, let's keep disallowing
                // if we were in a multi-finger gesture, until ACTION_UP.
                break;
            }
        }
        // If the scale detector is in progress, or we are actively panning, we consume the event.
        // Otherwise, return false to let the parent ScrollView handle it.
        return handledByScaleDetector || isZoomOrPanActive || getScale() > MIN_SCALE;
    }

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
                return false; // Pass the event to children (i.e., the DrawView)
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

            // Avoid invalid scale factors
            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor)) {
                return false;
            }

            // Apply scale, pivoting around the gesture focus.
            scaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            fixTranslation(); // Ensure we don't zoom outside the bounds
            invalidate();

            // Indicate that this gesture was handled for scaling
            isZoomOrPanActive = true;
            getParent().requestDisallowInterceptTouchEvent(true); // Ensure parent doesn't intercept during scale
            return true;
        }
    }

    // This is important for your DrawView
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

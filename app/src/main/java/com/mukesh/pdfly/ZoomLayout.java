package com.mukesh.pdfly;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

public class ZoomLayout extends FrameLayout {
    private enum Mode { NONE, DRAG, ZOOM }
    private Mode mode = Mode.NONE;
    private ScaleGestureDetector scaleDetector;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float[] lastEvent = null;
    private float scaleFactor = 1f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3f;

    // Values for matrix transformation
    private final float[] matrixValues = new float[9];
    private float translateX, translateY, scaleX, scaleY;

    public ZoomLayout(Context context) {
        this(context, null);
    }

    public ZoomLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        setBackgroundColor(0xFFEEEEEE); // Light grey background
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true; // Intercept all touch events
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = Mode.DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    float oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = Mode.ZOOM;
                    }
                    lastEvent = new float[4];
                    lastEvent[0] = event.getX(0);
                    lastEvent[1] = event.getX(1);
                    lastEvent[2] = event.getY(0);
                    lastEvent[3] = event.getY(1);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = Mode.NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == Mode.DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == Mode.ZOOM && event.getPointerCount() >= 2 && lastEvent != null) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / spacing(lastEvent[0], lastEvent[1], lastEvent[2], lastEvent[3]);
                        scaleFactor *= scale;
                        scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
                        matrix.postScale(scaleFactor, scaleFactor, mid.x, mid.y);
                    }
                }
                break;
        }

        applyMatrixToChildren();
        return true;
    }

    private void applyMatrixToChildren() {
        matrix.getValues(matrixValues);
        translateX = matrixValues[Matrix.MTRANS_X];
        translateY = matrixValues[Matrix.MTRANS_Y];
        scaleX = matrixValues[Matrix.MSCALE_X];
        scaleY = matrixValues[Matrix.MSCALE_Y];

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setTranslationX(translateX);
            child.setTranslationY(translateY);
            child.setScaleX(scaleX);
            child.setScaleY(scaleY);
        }
    }

    private float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0;
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float spacing(float x1, float x2, float y1, float y2) {
        float x = x1 - x2;
        float y = y1 - y2;
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        if (event.getPointerCount() < 2) return;
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
            matrix.setScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            applyMatrixToChildren();
            return true;
        }
    }
}
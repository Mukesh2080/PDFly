package com.mukesh.pdfly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

//hihi
public class DrawView extends View {
    private boolean drawingEnabled = false;

    private Path currentPath;
    private Paint currentPaint;

    private DrawSettingsProvider settingsProvider;

    public void setDrawingEnabled(boolean enabled) {
        this.drawingEnabled = enabled;
    }

    public void setDrawSettingsProvider(DrawSettingsProvider provider) {
        this.settingsProvider = provider;
    }

    private static class Stroke {
        Path path;
        Paint paint;

        Stroke(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private final Stack<Stroke> undoneStrokes = new Stack<>();

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            Stroke stroke = strokes.remove(strokes.size() - 1);
            undoneStrokes.push(stroke);
            invalidate();
        }
    }

    public void redo() {
        if (!undoneStrokes.isEmpty()) {
            Stroke stroke = undoneStrokes.pop();
            strokes.add(stroke);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Stroke stroke : strokes) {
            canvas.drawPath(stroke.path, stroke.paint);
        }
        if (currentPath != null && currentPaint != null) {
            canvas.drawPath(currentPath, currentPaint);
        }
    }

    private float lastX, lastY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!drawingEnabled || settingsProvider == null) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                currentPath = new Path();
                currentPath.moveTo(x, y);

                currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                currentPaint.setColor(settingsProvider.getCurrentPaintColor());
                currentPaint.setStrokeWidth(settingsProvider.getCurrentStrokeWidth());
                currentPaint.setStyle(Paint.Style.STROKE);
                currentPaint.setStrokeJoin(Paint.Join.ROUND);
                currentPaint.setStrokeCap(Paint.Cap.ROUND);

                lastX = x;
                lastY = y;

                undoneStrokes.clear();
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                float midX = (x + lastX) / 2;
                float midY = (y + lastY) / 2;
                currentPath.quadTo(lastX, lastY, midX, midY);
                lastX = x;
                lastY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }

                if (currentPath != null && currentPaint != null) {
                    strokes.add(new Stroke(currentPath, currentPaint));
                }
                currentPath = null;
                currentPaint = null;
                invalidate();
                return true;
        }

        return false;
    }
}

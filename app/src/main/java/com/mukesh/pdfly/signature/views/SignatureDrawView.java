package com.mukesh.pdfly.signature.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mukesh.pdfly.signature.activity.SignatureCreatorActivity;

public class SignatureDrawView extends View {

    private Path path = new Path();
    private Paint paint = new Paint();
    private Bitmap bitmap;
    private Canvas drawCanvas;

    private float lastX, lastY;
    private static final float TOUCH_TOLERANCE = 4;

    public interface SignatureDrawListener {
        void onStartDrawing();
        void onStopDrawing();
    }

    private SignatureDrawListener drawListener;
    private boolean isDrawing = false;

    public void setDrawListener(SignatureDrawListener listener) {
        this.drawListener = listener;
    }

    public SignatureDrawView(Context context) {
        super(context);
        init();
    }

    public SignatureDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(4f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        setBackgroundColor(Color.WHITE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(bitmap);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Disable ViewPager swiping when we start drawing
                if (getContext() instanceof SignatureCreatorActivity) {
                    ((SignatureCreatorActivity) getContext()).setSwipeEnabled(false);
                }
                path.moveTo(x, y);
                lastX = x;
                lastY = y;
                isDrawing = true;
                if (drawListener != null) drawListener.onStartDrawing();
                getParent().requestDisallowInterceptTouchEvent(true); // 🔥 Critical line
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - lastX);
                float dy = Math.abs(y - lastY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                    lastX = x;
                    lastY = y;
                }
                getParent().requestDisallowInterceptTouchEvent(true); // 🔥 Continue blocking
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                path.lineTo(lastX, lastY);
                drawCanvas.drawPath(path, paint);
                path.reset();
                if (isDrawing && drawListener != null) drawListener.onStopDrawing();
                isDrawing = false;
                // Re-enable ViewPager swiping when drawing ends
                if (getContext() instanceof SignatureCreatorActivity) {
                    ((SignatureCreatorActivity) getContext()).setSwipeEnabled(true);
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        invalidate();
        return true;
    }
    public void clear() {
        if (bitmap != null) {
            bitmap.eraseColor(Color.TRANSPARENT); // Make it fully transparent again
            invalidate();
        }
    }

    public Bitmap exportAsBitmap() {
        return bitmap;
    }
}

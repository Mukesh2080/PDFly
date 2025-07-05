package com.mukesh.pdfly.pdfrenderer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mukesh.pdfly.R;
import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;
import com.mukesh.pdfly.signature.views.OverlayElementView;

public class CheckmarkElementView extends OverlayElementView {
    private Drawable checkmarkDrawable;
    private Rect resizeHandleRect, rotateHandleRect, deleteHandleRect;
    private Paint selectionPaint;
    private boolean isSelected = false;
    private boolean isResizing = false, isRotating = false;
    private float dX, dY;
    private float rotationAngle = 0f;
    private static final int SELECTION_PADDING_DP = 30;
    private int selectionPaddingPx;
    private Drawable bgDrawable;

    public CheckmarkElementView(Context context) {
        super(context);
        setWillNotDraw(false);
        selectionPaddingPx = dpToPx(SELECTION_PADDING_DP);

        checkmarkDrawable = getTintedDrawable(R.drawable.checkmark); // <-- your checkmark vector
        bgDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.circle_button_bg);

        selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaint.setColor(Color.BLUE);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(3);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        initGestureListeners();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth(), h = getHeight();
        float centerX = w / 2f, centerY = h / 2f;
        int iconPadding = dpToPx(8);

        canvas.save();
        canvas.rotate(rotationAngle, centerX, centerY);

        // Draw checkmark drawable centered
        if (checkmarkDrawable != null) {
            int left = selectionPaddingPx + iconPadding;
            int top = selectionPaddingPx + iconPadding;
            int right = w - selectionPaddingPx - iconPadding;
            int bottom = h - selectionPaddingPx - iconPadding;
            checkmarkDrawable.setBounds(left, top, right, bottom);
            checkmarkDrawable.draw(canvas);
        }

        canvas.restore();

        if (isSelected) {
            canvas.drawRect(0, 0, w, h, selectionPaint);
            int iconSize = dpToPx(24);
            int padding = dpToPx(4);

            // Delete
            deleteHandleRect = drawHandle(canvas, R.drawable.ic_delete_24dp, 0, 0, iconSize, padding);
            // Rotate
            rotateHandleRect = drawHandle(canvas, R.drawable.ic_undo, w - iconSize, 0, iconSize, padding);
            // Resize
            resizeHandleRect = drawHandle(canvas, R.drawable.open_in_full_24dp, w - iconSize, h - iconSize, iconSize, padding);
        }
    }

    private Rect drawHandle(Canvas canvas, int iconResId, int left, int top, int size, int padding) {
        Rect bounds = new Rect(left, top, left + size, top + size);
        if (bgDrawable != null) {
            bgDrawable.setBounds(bounds);
            bgDrawable.draw(canvas);
        }

        Drawable icon = getTintedDrawable(iconResId);
        icon.setBounds(left + padding, top + padding, left + size - padding, top + size - padding);
        icon.draw(canvas);
        return bounds;
    }

    private void initGestureListeners() {
        setOnTouchListener((v, event) -> {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }

            float x = event.getX();
            float y = event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (resizeHandleRect != null && resizeHandleRect.contains((int) x, (int) y)) {
                        isResizing = true;
                        return true;
                    } else if (rotateHandleRect != null && rotateHandleRect.contains((int) x, (int) y)) {
                        isRotating = true;
                        return true;
                    } else if (deleteHandleRect != null && deleteHandleRect.contains((int) x, (int) y)) {
                        if (getContext() instanceof PdfEditorActivity) {
                            ((PdfEditorActivity) getContext()).removeOverlayElement(CheckmarkElementView.this);
                        }
                        return true;
                    } else {
                        if(!isSelected){
                            ((PdfEditorActivity) getContext()).onElementSelected(CheckmarkElementView.this);
                        }
                        dX = event.getRawX() - getX();
                        dY = event.getRawY() - getY();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isResizing) {
                        int newWidth = (int) x;
                        int newHeight = (int) y;
                        int minSize = dpToPx(20) + (2 * selectionPaddingPx);
                        newWidth = Math.max(minSize, newWidth);
                        newHeight = Math.max(minSize, newHeight);
                        ViewGroup.LayoutParams lp = getLayoutParams();
                        lp.width = newWidth;
                        lp.height = newHeight;
                        setLayoutParams(lp);
                        invalidate();
                    } else if (isRotating) {
                        float cx = getWidth() / 2f, cy = getHeight() / 2f;
                        rotationAngle = (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
                        invalidate();
                    } else {
                        setX(event.getRawX() - dX);
                        setY(event.getRawY() - dY);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isResizing = false;
                    isRotating = false;
                    break;
            }

            return true;
        });
    }

    private Drawable getTintedDrawable(@DrawableRes int drawableId) {
        Drawable d = AppCompatResources.getDrawable(getContext(), drawableId);
        if (d == null) return null;
        d = DrawableCompat.wrap(d.mutate());
        //DrawableCompat.setTint(d, Color.WHITE);
        return d;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    public void setSelectedState(boolean selected) {
        super.setSelectedState(selected);
        this.isSelected = selected;
        invalidate();
    }
}


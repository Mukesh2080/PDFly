package com.mukesh.pdfly.pdfrenderer.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mukesh.pdfly.R;
import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;
import com.mukesh.pdfly.signature.views.OverlayElementView;

public class TextElementView extends OverlayElementView {
    private static final int SELECTION_PADDING_DP = 30;
    private int selectionPaddingPx;

    private Paint selectionPaint;
    private boolean isSelected = false;
    private boolean isResizing = false;
    private boolean isRotating = false;
    private boolean isInMenuHandle = false;

    private Bitmap resizeIcon, rotateIcon, deleteIcon, menuIcon;
    private Rect resizeHandleRect, rotateHandleRect, deleteHandleRect, menuHandleRect;

    private float dX, dY;
    private float rotationAngle = 0f;

    private TextView textView;

    public TextElementView(Context context, String text, int textColor, float textSizeSp) {
        super(context);
        setWillNotDraw(false);
        selectionPaddingPx = dpToPx(SELECTION_PADDING_DP);
        initPaints();
        initIcons();
        initTextView(context, text, textColor, textSizeSp);
        initGestureListeners();
    }

    private void initTextView(Context context, String text, int textColor, float textSizeSp) {
        textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(textColor);
        textView.setTextSize(textSizeSp);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setPadding(10, 10, 10, 10);
        textView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        addView(textView);
    }

    private void initPaints() {
        selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaint.setColor(Color.BLUE);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(3);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
    }

    private void initIcons() {
        resizeIcon = getBitmapFromVectorDrawable(R.drawable.open_in_full_24dp);
        rotateIcon = getBitmapFromVectorDrawable(R.drawable.ic_undo);
        deleteIcon = getBitmapFromVectorDrawable(R.drawable.ic_delete_24dp);
        menuIcon = getBitmapFromVectorDrawable(R.drawable.ic_shape); // menu
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas); // Draw the child views first

        if (!isSelected) return;

        int w = getWidth();
        int h = getHeight();

        canvas.save();
        canvas.rotate(rotationAngle, w / 2f, h / 2f);

        canvas.drawRect(0, 0, w, h, selectionPaint);

        int iconSize = dpToPx(24);
        Drawable bgDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.circle_button_bg);

        deleteHandleRect = new Rect(0, 0, iconSize, iconSize);
        drawHandle(canvas, deleteHandleRect, deleteIcon, bgDrawable);

        rotateHandleRect = null; // You’ve hidden this already
//    rotateHandleRect = new Rect(w - iconSize, 0, w, iconSize);
//    drawHandle(canvas, rotateHandleRect, rotateIcon, bgDrawable);

        resizeHandleRect = new Rect(w - iconSize, h - iconSize, w, h);
        drawHandle(canvas, resizeHandleRect, resizeIcon, bgDrawable);

        menuHandleRect = new Rect(0, h - iconSize, iconSize, h);
        drawHandle(canvas, menuHandleRect, menuIcon, bgDrawable);

        canvas.restore();
    }


    private void drawHandle(Canvas canvas, Rect rect, Bitmap icon, Drawable bg) {
        bg.setBounds(rect);
        bg.draw(canvas);
        canvas.drawBitmap(icon, null, rect, null);
    }

    private void initGestureListeners() {
        setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isInMenuHandle = false;

                    if (resizeHandleRect != null && resizeHandleRect.contains((int) x, (int) y)) {
                        isResizing = true;
                        return true;
                    } else if (rotateHandleRect != null && rotateHandleRect.contains((int) x, (int) y)) {
                        isRotating = true;
                        return true;
                    } else if (deleteHandleRect != null && deleteHandleRect.contains((int) x, (int) y)) {
                        if (getContext() instanceof PdfEditorActivity) {
                            ((PdfEditorActivity) getContext()).removeOverlayElement(TextElementView.this);
                        }
                        return true;
                    } else if (menuHandleRect != null && menuHandleRect.contains((int) x, (int) y)) {
                        isInMenuHandle = true;
                        return true;
                    } else {
                        dX = event.getRawX() - getX();
                        dY = event.getRawY() - getY();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isInMenuHandle) return true;

                    if (isResizing) {
                        int newWidth = (int) x;
                        int newHeight = (int) y;
                        int minSize = dpToPx(10) + (2 * selectionPaddingPx);

                        newWidth = Math.max(minSize, newWidth);
                        newHeight = Math.max(minSize, newHeight);

                        ViewGroup.LayoutParams lp = getLayoutParams();
                        lp.width = newWidth;
                        lp.height = newHeight;
                        setLayoutParams(lp);
                        invalidate();
                    } else if (isRotating) {
                        float centerX = getWidth() / 2f;
                        float centerY = getHeight() / 2f;
                        float dx = x - centerX;
                        float dy = y - centerY;
                        rotationAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
                        invalidate();
                    } else {
                        if(!isSelected){
                            ((PdfEditorActivity) getContext()).onElementSelected(TextElementView.this);

                        }
                        setX(event.getRawX() - dX);
                        setY(event.getRawY() - dY);
                        if (getContext() instanceof PdfEditorActivity) {
                            ((PdfEditorActivity) getContext()).updateToolbarPosition(TextElementView.this);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isInMenuHandle) {
                        isInMenuHandle = false;
                        if (getContext() instanceof PdfEditorActivity) {
                            ((PdfEditorActivity) getContext()).showShapeToolbar(TextElementView.this);
                        }
                        return true;
                    }
                    isResizing = false;
                    isRotating = false;
                    break;
            }
            return true;
        });
    }

    private Bitmap getBitmapFromVectorDrawable(@DrawableRes int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(getContext(), drawableId);
        if (drawable == null) return null;
        drawable = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(drawable, Color.WHITE);
        int size = dpToPx(24);
        int padding = dpToPx(4);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(padding, padding, size - padding, size - padding);
        drawable.draw(canvas);
        return bitmap;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void setText(String newText) {
        textView.setText(newText);
    }

    @Override
    public void setColor(int color) {
        textView.setTextColor(color);
    }


    public void setSize(float sp) {
        textView.setTextSize(sp);
    }

    public void setBackgroundColorInt(int color) {
        textView.setBackgroundColor(color);
    }

    @Override
    public void setSelectedState(boolean selected) {
        super.setSelectedState(selected);
        this.isSelected = selected;
        invalidate();
    }

    public String getText() {
        return textView.getText().toString();
    }

    public float getTextSize() {
        return textView.getTextSize();
    }

    private static final float MIN_TEXT_SIZE = 8f; // Minimum text size in sp
    private static final float MAX_TEXT_SIZE = 72f; // Maximum text size in sp
    private static final float SIZE_INCREMENT = 2f; // Size change step

    @Override
    public void increaseSize() {
        float currentSize = getTextSize();
        float newSize = currentSize + SIZE_INCREMENT;

        if (newSize <= MAX_TEXT_SIZE) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
            invalidate();
        } else {
            // Optional: Show message or visual feedback when max size reached
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, MAX_TEXT_SIZE);
            invalidate();
        }
    }

    @Override
    public void decreaseSize() {
        float currentSize = getTextSize();
        float newSize = currentSize - SIZE_INCREMENT;

        if (newSize >= MIN_TEXT_SIZE) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
            invalidate();
        } else {
            // Optional: Show message or visual feedback when min size reached
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, MIN_TEXT_SIZE);
            invalidate();
        }
    }

    // Toggle bold style
    public void setBold(boolean enable) {
        textView.setTypeface(null, enable ? Typeface.BOLD : Typeface.NORMAL);
        invalidate();
    }

    // Toggle underline
    public void setUnderline(boolean enable) {
        textView.setPaintFlags(enable ?
                textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG :
                textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        invalidate();
    }

    // Toggle strikethrough
    public void setStrikethrough(boolean enable) {
        textView.setPaintFlags(enable ?
                textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG :
                textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        invalidate();
    }

    // Get current style states
    public boolean isBold() {
        return textView.getTypeface() != null && textView.getTypeface().isBold();
    }

    public boolean isUnderline() {
        return (textView.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) != 0;
    }

    public boolean isStrikethrough() {
        return (textView.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) != 0;
    }
}


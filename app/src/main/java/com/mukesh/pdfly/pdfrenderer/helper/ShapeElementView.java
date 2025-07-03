package com.mukesh.pdfly.pdfrenderer.helper;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mukesh.pdfly.R;
import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;
import com.mukesh.pdfly.signature.views.OverlayElementView;

import java.util.ArrayList;
import java.util.List;

public class ShapeElementView extends OverlayElementView {
    private PdfEditorActivity.ShapeType shapeType;

    private Bitmap resizeIcon, rotateIcon, deleteIcon;
    private Rect resizeHandleRect, rotateHandleRect, deleteHandleRect;

    private Paint shapePaint, selectionPaint;
    private boolean isSelected = false;
    private boolean isResizing = false, isRotating = false;

    private float dX, dY;
    private float rotationAngle = 0f;
    private static final int SELECTION_PADDING_DP = 30; // Increased padding for handles
    private int selectionPaddingPx;
    private Bitmap menuIcon;
    private Rect menuHandleRect;
    private boolean isInMenuHandle = false;



    public ShapeElementView(Context context, PdfEditorActivity.ShapeType type, int color, float stroke) {
        super(context);
        selectionPaddingPx = dpToPx(SELECTION_PADDING_DP);
        this.shapeType = type;
        setWillNotDraw(false);

        // Load icons
        resizeIcon = getBitmapFromVectorDrawable(R.drawable.open_in_full_24dp);
        rotateIcon = getBitmapFromVectorDrawable(R.drawable.ic_undo); // Your vector drawable
        deleteIcon = getBitmapFromVectorDrawable(R.drawable.ic_delete_24dp);
        menuIcon = getBitmapFromVectorDrawable(R.drawable.ic_shape); // Your vector drawable

        // Paints
        shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shapePaint.setColor(color);
        shapePaint.setStrokeWidth(stroke);
        shapePaint.setStyle(Paint.Style.STROKE);

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

        int w = getWidth();
        int h = getHeight();

        // Calculate the effective drawing area for the shape (inside padding)
        float shapeLeft = selectionPaddingPx;
        float shapeTop = selectionPaddingPx;
        float shapeRight = w - selectionPaddingPx;
        float shapeBottom = h - selectionPaddingPx;

        // Calculate center for rotation
        float centerX = w / 2f;
        float centerY = h / 2f;

        canvas.save();
        canvas.rotate(rotationAngle, centerX, centerY); // Rotate around the view's center

        // Draw the shape within the padded area
        switch (shapeType) {
            case RECTANGLE:
                canvas.drawRect(shapeLeft, shapeTop, shapeRight, shapeBottom, shapePaint);
                break;
            case CIRCLE:
                float radius = Math.min(shapeRight - shapeLeft, shapeBottom - shapeTop) / 2f;
                canvas.drawCircle(shapeLeft + radius, shapeTop + radius, radius, shapePaint);
                break;
            case LINE:
                canvas.drawLine(shapeLeft, shapeBottom, shapeRight, shapeTop, shapePaint);
                break;
        }

        canvas.restore();
        Drawable bgDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.circle_button_bg);

        if (isSelected) {
            // Draw selection border around the entire view (or slightly inset)
            canvas.drawRect(0, 0, w, h, selectionPaint);

            int iconSize = dpToPx(24);
            int padding = dpToPx(4); // Optional inner padding

            // Delete (top-left, outside the shape)
            deleteHandleRect = new Rect(0, 0, iconSize, iconSize);
            bgDrawable.setBounds(deleteHandleRect);
            bgDrawable.draw(canvas);
            canvas.drawBitmap(deleteIcon, null, deleteHandleRect, null);

            // Rotate (top-right, outside the shape)
            rotateHandleRect = new Rect(w - iconSize, 0, w, iconSize);
            bgDrawable.setBounds(rotateHandleRect);
            bgDrawable.draw(canvas);
            canvas.drawBitmap(rotateIcon, null, rotateHandleRect, null);

            // Resize (bottom-right, outside the shape)
            resizeHandleRect = new Rect(w - iconSize, h - iconSize, w, h);
            bgDrawable.setBounds(resizeHandleRect);
            bgDrawable.draw(canvas);
            canvas.drawBitmap(resizeIcon, null, resizeHandleRect, null);

            // Menu (bottom-left, outside the shape)
            menuHandleRect = new Rect(0, h - iconSize, iconSize, h);
            bgDrawable.setBounds(menuHandleRect);
            bgDrawable.draw(canvas);
            canvas.drawBitmap(menuIcon, null, menuHandleRect, null);


            // You might want to adjust the handle positions if you want them to rotate with the shape,
            // but for a static control, placing them at the corners of the *view* is often simpler.
            // If you want them to follow the rotation, you'd need to apply inverse rotations to their drawing.
        }
    }

    @Override
    public void setSelectedState(boolean selected) {
        super.setSelectedState(selected);
        this.isSelected = selected;
        invalidate();
    }

    private void initGestureListeners() {
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (getContext() instanceof PdfEditorActivity) {
                    ((PdfEditorActivity) getContext()).onElementSelected(ShapeElementView.this);
                }
                return true;
            }
        });

        setOnTouchListener((v, event) -> {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }

            float x = event.getX();
            float y = event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isInMenuHandle = false;

                    if (resizeHandleRect != null && resizeHandleRect.contains((int) x, (int) y)) {
                        isResizing = true;
                        return  true;
                    } else if (rotateHandleRect != null && rotateHandleRect.contains((int) x, (int) y)) {
                        isRotating = true;
                        return  true;
                    } else if (deleteHandleRect != null && deleteHandleRect.contains((int) x, (int) y)) {
                        if (getContext() instanceof PdfEditorActivity) {
                            ((PdfEditorActivity) getContext()).removeOverlayElement(ShapeElementView.this);
                        }
                        return true;
                    } else if (menuHandleRect != null && menuHandleRect.contains((int) x, (int) y)) {
                        isInMenuHandle = true;
                        return true; // Prevent any dragging or selection
                    } else {
                        dX = event.getRawX() - getX();
                        dY = event.getRawY() - getY();
                    }
                    break;


                case MotionEvent.ACTION_MOVE:
                    if (isInMenuHandle) {
                        return true;
                    }
                    if (isResizing) {
                        // New width and height for the *entire view*, including padding
                        int newWidth = (int) x ; // Add padding back for the view's size
                        int newHeight = (int) y ; // Add padding back for the view's size

                        int minSize = dpToPx(50) + (2 * selectionPaddingPx); // Minimum size for the view
                        newWidth = Math.max(minSize, newWidth);
                        newHeight = Math.max(minSize, newHeight);

                        //View parent = (View) getParent();
//                        if (parent != null) {
//                            int parentWidth = parent.getWidth();
//                            int parentHeight = parent.getHeight();
//
//                            // Clamp width and height so shape stays within the page
//                            newWidth = Math.min(newWidth, parentWidth - getLeft());
//                            newHeight = Math.min(newHeight, parentHeight - getTop());
//                        }

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
                        setX(event.getRawX() - dX);
                        setY(event.getRawY() - dY);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isInMenuHandle) {
                        isInMenuHandle = false;
                        if (getContext() instanceof PdfEditorActivity) {
                            ((PdfEditorActivity) getContext()).showShapeToolbar(ShapeElementView.this);
                        }
                        return true;
                    }
                    isResizing = false;
                    isRotating = false;
                    break;
            }
            gestureDetector.onTouchEvent(event);

            return true;
        });
    }

    private Bitmap getBitmapFromVectorDrawable(@DrawableRes int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(getContext(), drawableId);
        if (drawable == null) return null;
        drawable = DrawableCompat.wrap(drawable.mutate()); // Required for proper tinting
        DrawableCompat.setTint(drawable, Color.parseColor("white")); // Set to desired color
        int size = dpToPx(24);
        int padding = dpToPx(4); // padding inside the box

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(padding, padding, size - padding, size - padding);
        drawable.draw(canvas);
        return bitmap;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    public void setStrokeWidth(float width) {
        shapePaint.setStrokeWidth(width);
        invalidate();
    }

    @Override
    public void setColor(int color) {
        shapePaint.setColor(color);
        invalidate();
    }

    public float getStrokeWidth() {
        return shapePaint.getStrokeWidth();
    }


}



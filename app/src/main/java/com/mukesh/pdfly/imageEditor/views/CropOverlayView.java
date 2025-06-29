package com.mukesh.pdfly.imageEditor.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class CropOverlayView extends View {

    private static final String TAG = "CropOverlayView";

    // Paint objects for drawing the overlay, crop rectangle, grid, and handles
    private Paint overlayPaint;
    private Paint cropRectPaint;
    private Paint gridPaint;
    private Paint handlePaint;
    private Paint clearPaint; // New paint for clearing the crop area in the overlay layer

    // The current crop rectangle in this view's (screen) coordinates
    private RectF cropRect = new RectF();

    // Image and ImageView references
    private Bitmap imageBitmap;
    private ImageView imageView;
    // The actual bounds of the displayed image within the ImageView (after scaling/fitCenter)
    private RectF imageRectInView = new RectF();

    // Touch handling variables
    private int activePointerId = MotionEvent.INVALID_POINTER_ID; // Used for multi-touch (though only single touch is supported for drag/resize)
    private float lastTouchX, lastTouchY; // Last recorded touch coordinates
    private int touchRegion = NONE; // Indicates which part of the crop rectangle is being manipulated

    // Constants for touch regions
    private static final int NONE = 0;
    private static final int MOVE = 1; // Moving the entire crop rectangle
    private static final int TOP_LEFT = 2; // Resizing from top-left corner
    private static final int TOP_RIGHT = 3; // Resizing from top-right corner
    private static final int BOTTOM_LEFT = 4; // Resizing from bottom-left corner
    private static final int BOTTOM_RIGHT = 5; // Resizing from bottom-right corner
    private static final int TOP = 6;    // Resizing from top edge
    private static final int BOTTOM = 7; // Resizing from bottom edge
    private static final int LEFT = 8;   // Resizing from left edge
    private static final int RIGHT = 9;  // Resizing from right edge

    // Touch tolerance for detecting if a handle/border is touched (in DP, converted to Px)
    private static final float TOUCH_TOLERANCE_DP = 24;
    private float touchTolerancePx; // Converted to pixels based on screen density

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Calculate the touch tolerance in pixels for consistent behavior across devices
        touchTolerancePx = TOUCH_TOLERANCE_DP * getResources().getDisplayMetrics().density;

        // Initialize paints
        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#99000000")); // Semi-transparent black for the overlay
        // NOTE: No Xfermode set for overlayPaint initially, it just draws its color.

        cropRectPaint = new Paint();
        cropRectPaint.setColor(Color.WHITE); // White border for the crop rectangle
        cropRectPaint.setStyle(Paint.Style.STROKE); // Only draw the outline
        cropRectPaint.setStrokeWidth(3f); // Thickness of the border

        gridPaint = new Paint();
        gridPaint.setColor(Color.WHITE);
        gridPaint.setAlpha(120); // Semi-transparent white for the grid lines
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f); // Thin grid lines

        handlePaint = new Paint();
        handlePaint.setColor(Color.CYAN); // Bright color for resize handles
        handlePaint.setStyle(Paint.Style.FILL); // Fill the handle circles

        // Initialize a dedicated paint for clearing (punching the hole)
        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    /**
     * Sets the Bitmap currently displayed by the ImageView and a reference to the ImageView itself.
     * This is crucial for determining the actual display size and position of the image within the view.
     * Must be called whenever the image changes or after rotation.
     * @param bitmap The bitmap currently loaded in the ImageView.
     * @param view The ImageView instance holding the bitmap.
     */
    public void setImage(Bitmap bitmap, ImageView view) {
        this.imageBitmap = bitmap;
        this.imageView = view;
        calculateImageDisplayBounds(); // Recalculate where the image is actually drawn

        // If the image is valid and the cropRect is empty (first load or after reset)
        if (imageRectInView.width() > 0 && imageRectInView.height() > 0 && cropRect.isEmpty()) {
            // Initialize crop rect to be slightly inset from the image bounds
            float inset = Math.min(imageRectInView.width(), imageRectInView.height()) * 0.1f;
            cropRect.set(
                    imageRectInView.left + inset,
                    imageRectInView.top + inset,
                    imageRectInView.right - inset,
                    imageRectInView.bottom - inset
            );
        } else if (imageRectInView.width() > 0 && imageRectInView.height() > 0 && !cropRect.isEmpty()) {
            // If image is reloaded/rotated and cropRect already exists,
            // attempt to scale/reposition the existing cropRect relative to new image bounds.
            // This is a complex step for maintaining relative crop position after rotation.
            // For simplicity, we'll just reset it to a default inset for now.
            // A more advanced implementation would map the old cropRect relative to the old imageRectInView
            // and then map that ratio to the new imageRectInView.
            float inset = Math.min(imageRectInView.width(), imageRectInView.height()) * 0.1f;
            cropRect.set(
                    imageRectInView.left + inset,
                    imageRectInView.top + inset,
                    imageRectInView.right - inset,
                    imageRectInView.bottom - inset
            );
        } else {
            cropRect.setEmpty(); // Clear crop rect if no valid image or bounds
        }
        invalidate(); // Request redraw
    }

    /**
     * Calculates the actual bounding rectangle of the image as it's displayed within the ImageView.
     * This takes into account the ImageView's scaleType (e.g., fitCenter, centerCrop) and padding.
     */
    private void calculateImageDisplayBounds() {
        if (imageView == null || imageBitmap == null) {
            imageRectInView.setEmpty(); // Clear if no image or ImageView
            return;
        }

        // Get the matrix that ImageView uses to scale and position its drawable
        Matrix matrix = imageView.getImageMatrix();
        // Create a RectF representing the original bitmap's dimensions
        RectF drawableRect = new RectF(0, 0, imageBitmap.getWidth(), imageBitmap.getHeight());
        // Map the drawableRect through the ImageView's matrix to get its display bounds in view coordinates
        matrix.mapRect(imageRectInView, drawableRect);
        Log.d(TAG, "Image display bounds in view: " + imageRectInView.toString());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate image display bounds if the view's size changes (e.g., orientation change)
        calculateImageDisplayBounds();
        // If cropRect was empty (e.g., initial load before layout), initialize it now.
        if (imageRectInView.width() > 0 && imageRectInView.height() > 0 && cropRect.isEmpty()) {
            float inset = Math.min(imageRectInView.width(), imageRectInView.height()) * 0.1f;
            cropRect.set(
                    imageRectInView.left + inset,
                    imageRectInView.top + inset,
                    imageRectInView.right - inset,
                    imageRectInView.bottom - inset
            );
        }
        invalidate(); // Request redraw
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Don't draw anything if there's no image or a valid crop rectangle
        if (imageBitmap == null || imageRectInView.isEmpty() || cropRect.isEmpty()) {
            return;
        }

        // --- Start of the fix for semi-transparent overlay ---
        // Create a new layer on the canvas. This layer is initially transparent.
        // All drawing operations after saveLayer will go onto this temporary layer
        // until restoreToCount is called.
        int layerId = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

        // 1. Draw the semi-transparent black overlay over the entire layer.
        // This fills the temporary layer with semi-transparent black.
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        // 2. Draw the crop rectangle on this layer using PorterDuff.Mode.CLEAR.
        // This "punches a hole" through the semi-transparent black already drawn on the layer.
        // The area of cropRect on the layer becomes completely transparent.
        canvas.drawRect(cropRect, clearPaint);

        // Restore the canvas layer. This composites the temporary layer (semi-transparent black
        // with a transparent hole) onto the main canvas, blending it with the ImageView content below.
        canvas.restoreToCount(layerId);
        // --- End of the fix ---


        // The rest of the drawing (border, grid, handles) should be done on the main canvas
        // AFTER the layer is restored, so they are drawn on top of the cut-out overlay.

        // 3. Draw the crop rectangle border
        canvas.drawRect(cropRect, cropRectPaint);

        // 4. Draw the 3x3 grid lines inside the crop rectangle
        float hThird = cropRect.width() / 3f;
        float vThird = cropRect.height() / 3f;
        for (int i = 1; i < 3; i++) {
            // Vertical grid lines
            canvas.drawLine(cropRect.left + hThird * i, cropRect.top,
                    cropRect.left + hThird * i, cropRect.bottom, gridPaint);
            // Horizontal grid lines
            canvas.drawLine(cropRect.left, cropRect.top + vThird * i,
                    cropRect.right, cropRect.top + vThird * i, gridPaint);
        }

        // 5. Draw corner handles (circles)
        float handleRadius = touchTolerancePx / 2.5f; // Adjust handle size visually
        canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint);

        // Optional: Draw mid-edge handles for more precise resizing
        canvas.drawCircle(cropRect.centerX(), cropRect.top, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.centerX(), cropRect.bottom, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.left, cropRect.centerY(), handleRadius, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.centerY(), handleRadius, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Do not process touch events if no image is set or cropRect is invalid
        if (imageBitmap == null || imageRectInView.isEmpty() || cropRect.isEmpty()) {
            return false;
        }

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Get the ID of the first pointer that touched down
                activePointerId = event.getPointerId(0);
                // Record the initial touch coordinates
                lastTouchX = event.getX();
                lastTouchY = event.getY();

                // Determine which region of the crop rectangle was touched
                touchRegion = getTouchRegion(lastTouchX, lastTouchY);
                if (touchRegion != NONE) {
                    // If a valid region was touched, consume the event (return true)
                    // and prevent parent views from intercepting.
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer
                final int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    return false; // Should not happen if activePointerId is valid
                }

                float currentX = event.getX(pointerIndex);
                float currentY = event.getY(pointerIndex);

                // Calculate movement delta
                float dx = currentX - lastTouchX;
                float dy = currentY - lastTouchY;

                if (touchRegion == NONE) {
                    // If no specific region was identified on ACTION_DOWN,
                    // do not move or resize. This prevents accidental dragging
                    // when touching outside the crop handles/border.
                    break;
                }

                // Update the crop rectangle based on the identified touch region and movement
                updateCropRect(dx, dy);

                // Update last touch coordinates for the next move event
                lastTouchX = currentX;
                lastTouchY = currentY;
                invalidate(); // Request a redraw to show the updated crop rectangle
                return true; // Consume the event
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                // Reset active pointer and touch region
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                touchRegion = NONE;
                // Allow parent views to intercept touch events again
                getParent().requestDisallowInterceptTouchEvent(false);
                return true; // Consume the event
            }
        }
        // If no region was touched or event was not handled, return false
        // to allow parent views (if any) to process the event.
        return false;
    }

    /**
     * Determines which part of the crop rectangle (or its handles) was touched.
     * @param x Touch X coordinate.
     * @param y Touch Y coordinate.
     * @return An integer constant indicating the touched region (e.g., TOP_LEFT, MOVE, NONE).
     */
    private int getTouchRegion(float x, float y) {
        // Check corner handles first for wider touch areas
        float left = cropRect.left;
        float top = cropRect.top;
        float right = cropRect.right;
        float bottom = cropRect.bottom;

        // Top-left corner
        if (x >= left - touchTolerancePx && x <= left + touchTolerancePx &&
                y >= top - touchTolerancePx && y <= top + touchTolerancePx) {
            return TOP_LEFT;
        }
        // Top-right corner
        if (x >= right - touchTolerancePx && x <= right + touchTolerancePx &&
                y >= top - touchTolerancePx && y <= top + touchTolerancePx) {
            return TOP_RIGHT;
        }
        // Bottom-left corner
        if (x >= left - touchTolerancePx && x <= left + touchTolerancePx &&
                y >= bottom - touchTolerancePx && y <= bottom + touchTolerancePx) {
            return BOTTOM_LEFT;
        }
        // Bottom-right corner
        if (x >= right - touchTolerancePx && x <= right + touchTolerancePx &&
                y >= bottom - touchTolerancePx && y <= bottom + touchTolerancePx) {
            return BOTTOM_RIGHT;
        }

        // Check edges (mid-points) for resizing
        // Top edge
        if (x >= left + touchTolerancePx && x <= right - touchTolerancePx &&
                y >= top - touchTolerancePx && y <= top + touchTolerancePx) {
            return TOP;
        }
        // Bottom edge
        if (x >= left + touchTolerancePx && x <= right - touchTolerancePx &&
                y >= bottom - touchTolerancePx && y <= bottom + touchTolerancePx) {
            return BOTTOM;
        }
        // Left edge
        if (x >= left - touchTolerancePx && x <= left + touchTolerancePx &&
                y >= top + touchTolerancePx && y <= bottom - touchTolerancePx) {
            return LEFT;
        }
        // Right edge
        if (x >= right - touchTolerancePx && x <= right + touchTolerancePx &&
                y >= top + touchTolerancePx && y <= bottom - touchTolerancePx) {
            return RIGHT;
        }

        // Check if inside the crop rectangle (for moving the entire rectangle)
        if (cropRect.contains(x, y)) {
            return MOVE;
        }

        return NONE; // No valid region touched
    }

    /**
     * Updates the crop rectangle's dimensions and position based on touch movement.
     * Clamps the crop rectangle within the image display bounds.
     * @param dx Change in X coordinate.
     * @param dy Change in Y coordinate.
     */
    private void updateCropRect(float dx, float dy) {
        float newLeft = cropRect.left;
        float newTop = cropRect.top;
        float newRight = cropRect.right;
        float newBottom = cropRect.bottom;

        // Minimum allowed size for the crop rectangle
        float minCropSize = touchTolerancePx * 2; // e.g., 48dp minimum size

        switch (touchRegion) {
            case MOVE:
                // Move the entire rectangle
                newLeft += dx;
                newTop += dy;
                newRight += dx;
                newBottom += dy;
                break;
            case TOP_LEFT:
                newLeft += dx;
                newTop += dy;
                break;
            case TOP_RIGHT:
                newRight += dx;
                newTop += dy;
                break;
            case BOTTOM_LEFT:
                newLeft += dx;
                newBottom += dy;
                break;
            case BOTTOM_RIGHT:
                newRight += dx;
                newBottom += dy;
                break;
            case TOP:
                newTop += dy;
                break;
            case BOTTOM:
                newBottom += dy;
                break;
            case LEFT:
                newLeft += dx;
                break;
            case RIGHT:
                newRight += dx;
                break;
        }

        // Clamp crop rectangle to stay within the image display bounds
        // This is crucial to prevent the crop area from going outside the visible image.
        if (newLeft < imageRectInView.left) newLeft = imageRectInView.left;
        if (newTop < imageRectInView.top) newTop = imageRectInView.top;
        if (newRight > imageRectInView.right) newRight = imageRectInView.right;
        if (newBottom > imageRectInView.bottom) newBottom = imageRectInView.bottom;

        // Ensure minimum size (width and height)
        if (newRight - newLeft < minCropSize) {
            if (touchRegion == LEFT || touchRegion == TOP_LEFT || touchRegion == TOP_RIGHT || touchRegion == BOTTOM_LEFT || touchRegion == BOTTOM_RIGHT) {
                // If moving left edge or corner, adjust newLeft
                newLeft = newRight - minCropSize;
            } else if (touchRegion == RIGHT) {
                // If moving right edge, adjust newRight
                newRight = newLeft + minCropSize;
            }
            // For TOP/BOTTOM, no horizontal change
        }
        if (newBottom - newTop < minCropSize) {
            if (touchRegion == TOP || touchRegion == TOP_LEFT || touchRegion == TOP_RIGHT || touchRegion == BOTTOM_LEFT || touchRegion == BOTTOM_RIGHT) {
                // If moving top edge or corner, adjust newTop
                newTop = newBottom - minCropSize;
            } else if (touchRegion == BOTTOM) {
                // If moving bottom edge, adjust newBottom
                newBottom = newTop + minCropSize;
            }
            // For LEFT/RIGHT, no vertical change
        }


        // Special handling for MOVE to ensure it stays within bounds
        if (touchRegion == MOVE) {
            float currentWidth = cropRect.width();
            float currentHeight = cropRect.height();

            if (newLeft < imageRectInView.left) {
                newLeft = imageRectInView.left;
                newRight = newLeft + currentWidth;
            }
            if (newTop < imageRectInView.top) {
                newTop = imageRectInView.top;
                newBottom = newTop + currentHeight;
            }
            if (newRight > imageRectInView.right) {
                newRight = imageRectInView.right;
                newLeft = newRight - currentWidth;
            }
            if (newBottom > imageRectInView.bottom) {
                newBottom = imageRectInView.bottom;
                newTop = newBottom - currentHeight;
            }
        }


        // Update the cropRect with the new clamped values
        cropRect.set(newLeft, newTop, newRight, newBottom);
    }

    /**
     * Returns the current crop rectangle's coordinates relative to the original image bitmap's pixels.
     * This is the essential method to call from ImageEditActivity to get the final crop bounds.
     * @return A RectF representing the crop area in the original image's pixel coordinates.
     * Returns null if no image is loaded or bounds are invalid.
     */
    public RectF getCropRectInImageCoordinates() {
        if (imageBitmap == null || imageRectInView.isEmpty() || cropRect.isEmpty()) {
            return null;
        }

        // Calculate the scale factor of the displayed image relative to its original size
        float scaleX = imageRectInView.width() / imageBitmap.getWidth();
        float scaleY = imageRectInView.height() / imageBitmap.getHeight();

        // Calculate the offset of the displayed image from the top-left of the ImageView
        float offsetX = imageRectInView.left;
        float offsetY = imageRectInView.top;

        // Convert cropRect (which is in view coordinates) to bitmap pixel coordinates
        float cropLeftInImage = (cropRect.left - offsetX) / scaleX;
        float cropTopInImage = (cropRect.top - offsetY) / scaleY;
        float cropRightInImage = (cropRect.right - offsetX) / scaleX;
        float cropBottomInImage = (cropRect.bottom - offsetY) / scaleY;

        // Create and return the RectF in image pixel coordinates
        return new RectF(cropLeftInImage, cropTopInImage, cropRightInImage, cropBottomInImage);
    }
}

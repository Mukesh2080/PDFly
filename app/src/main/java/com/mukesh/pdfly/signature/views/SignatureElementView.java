package com.mukesh.pdfly.signature.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;
import com.mukesh.pdfly.R;

public class SignatureElementView extends OverlayElementView {
    private ImageView signatureImage;
    private ImageButton deleteButton;
    private View resizeHandle;
    private View rotateHandle;

    private float lastTouchX, lastTouchY;
    private boolean isResizing = false;
    private boolean isRotating = false;
    private float initialRotation = 0f;
    private View signatureContainer;
    private View signatureBackground;


    public SignatureElementView(Context context, Bitmap signatureBitmap) {
        super(context);
        initView(signatureBitmap);
    }

    private void initView(Bitmap signatureBitmap) {
        LayoutInflater.from(getContext()).inflate(R.layout.view_signature_element, this, true);
        setClipChildren(false);
        setClipToPadding(false);

        signatureBackground = findViewById(R.id.signatureBackground);
        signatureContainer = findViewById(R.id.signatureContainer);
        signatureImage = findViewById(R.id.signatureImage);
        deleteButton = findViewById(R.id.deleteIcon);
        resizeHandle = findViewById(R.id.resizeHandle);
        rotateHandle = findViewById(R.id.rotateHandle);
        //signatureBackground.setBackgroundResource(R.drawable.bg_element_selected);
        signatureImage.setImageBitmap(signatureBitmap);
        setupTouchListeners();
    }
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void setupTouchListeners() {
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            private float initialDist;
            private float initialScaleX, initialScaleY;
            private float centerX, centerY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }

                        // Get center of this view on screen
                        centerX = getX() + getWidth() / 2f;
                        centerY = getY() + getHeight() / 2f;

                        initialDist = (float) Math.sqrt(
                                Math.pow(event.getRawX() - centerX, 2) +
                                        Math.pow(event.getRawY() - centerY, 2)
                        );

                        initialScaleX = getScaleX();
                        initialScaleY = getScaleY();

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float currentDist = (float) Math.sqrt(
                                Math.pow(event.getRawX() - centerX, 2) +
                                        Math.pow(event.getRawY() - centerY, 2)
                        );

                        float scale = currentDist / initialDist;

                        // Apply uniform scale, clamp if needed
                        float newScale = Math.max(0.3f, Math.min(5f, initialScaleX * scale));
                        setScaleX(newScale);
                        setScaleY(newScale);
                        //keep buttons size
                        float minScale = 0.8f; // Prevent scaling too small
                        float maxScale = 4.0f; // Optional: limit very large scale
                        newScale = Math.max(minScale, Math.min(maxScale, newScale));

                        deleteButton.setScaleX(1f / newScale);
                        deleteButton.setScaleY(1f / newScale);

                        resizeHandle.setScaleX(1f / newScale);
                        resizeHandle.setScaleY(1f / newScale);

                        rotateHandle.setScaleX(1f / newScale);
                        rotateHandle.setScaleY(1f / newScale);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        return true;
                }
                return false;
            }
        });
        rotateHandle.setOnTouchListener(new View.OnTouchListener() {
            private float lastAngle;
            private float startAngle;
            private float centerX, centerY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Prevent parent from intercepting touch events
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }

                        // Calculate center point of the view
                        centerX = getX() + getWidth() / 2f;
                        centerY = getY() + getHeight() / 2f;

                        // Get initial angle between touch point and center
                        startAngle = (float) Math.toDegrees(Math.atan2(
                                event.getRawY() - centerY,
                                event.getRawX() - centerX
                        ));

                        // Store current rotation
                        lastAngle = getRotation();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Calculate current angle
                        float currentAngle = (float) Math.toDegrees(Math.atan2(
                                event.getRawY() - centerY,
                                event.getRawX() - centerX
                        ));

                        // Apply rotation difference
                        setRotation(lastAngle + (currentAngle - startAngle));
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Clean up
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        return true;
                }
                return false;
            }
        });
        deleteButton.setOnClickListener(v -> {
            ((ViewGroup) getParent()).removeView(this);
            if (getContext() instanceof PdfEditorActivity) {
                ((PdfEditorActivity) getContext()).removeOverlayElement(this);
            }
        });
    }

    @Override
    public void setSelectedState(boolean selected) {
        super.setSelectedState(selected);
        deleteButton.setVisibility(selected ? VISIBLE : GONE);
        resizeHandle.setVisibility(selected ? VISIBLE : GONE);
        rotateHandle.setVisibility(selected ? VISIBLE : GONE);
        signatureContainer.setBackgroundResource(selected? R.drawable.bg_element_selected: Color.TRANSPARENT);

    }
}
package com.mukesh.pdfly.pdfrenderer.helper;
import android.app.Dialog;

import com.google.android.material.slider.Slider;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.mukesh.pdfly.R;
public class PenSettingsDialogHelper {

    public interface OnPenSettingsChangedListener {
        void onColorChanged(int color);
        void onStrokeWidthChanged(float strokeWidth);
    }

    private final Activity activity;
    private final OnPenSettingsChangedListener listener;

    private int selectedColor;
    private float strokeWidth;

    public PenSettingsDialogHelper(Activity activity, int initialColor, float initialStrokeWidth,
                                   OnPenSettingsChangedListener listener) {
        this.activity = activity;
        this.selectedColor = initialColor;
        this.strokeWidth = initialStrokeWidth;
        this.listener = listener;
    }

    public void show() {
        Dialog dialog = new Dialog(activity);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_pen_selector, null);

        FrameLayout wrapper = new FrameLayout(activity);
        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics());
        wrapParams.setMargins(margin, margin, margin, margin);
        sheetView.setLayoutParams(wrapParams);
        wrapper.addView(sheetView);
        dialog.setContentView(wrapper);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            wlp.gravity = Gravity.BOTTOM;
            wlp.y = 230;
            window.setAttributes(wlp);
        }

        // Stroke Width UI
        TextView paintSize = sheetView.findViewById(R.id.paint_size);
        Slider strokeSlider = sheetView.findViewById(R.id.strokeSlider);
        paintSize.setText((int) strokeWidth + "");
        strokeSlider.setValue(strokeWidth);
        strokeSlider.addOnChangeListener((slider, value, fromUser) -> {
            strokeWidth = value;
            paintSize.setText(String.valueOf((int) strokeWidth));
            listener.onStrokeWidthChanged(strokeWidth);
        });

        // Color Row
        LinearLayout colorRow = sheetView.findViewById(R.id.colorRow);
        int[] colors = new int[]{Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA};
        colorRow.removeAllViews();

        for (int color : colors) {
            View colorCircle = new View(activity);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(72, 72);
            params.setMargins(16, 8, 16, 8);
            colorCircle.setLayoutParams(params);
            colorCircle.setTag(color);

            if (color == selectedColor) {
                tintSelectedColorCircle(colorCircle, color);
            } else {
                tintUnselectedColorCircle(colorCircle, color);
            }

            colorCircle.setOnClickListener(v -> {
                selectedColor = (int) v.getTag();
                listener.onColorChanged(selectedColor);

                // Update all color buttons
                for (int i = 0; i < colorRow.getChildCount(); i++) {
                    View c = colorRow.getChildAt(i);
                    int cColor = (int) c.getTag();
                    if (cColor == selectedColor) {
                        tintSelectedColorCircle(c, cColor);
                    } else {
                        tintUnselectedColorCircle(c, cColor);
                    }
                }
            });

            colorRow.addView(colorCircle);
        }

        dialog.show();
    }

    private void tintSelectedColorCircle(View view, int color) {
        view.setBackgroundResource(R.drawable.bg_color_circle_selected);
        Drawable bg = view.getBackground();

        if (bg instanceof LayerDrawable) {
            Drawable inner = ((LayerDrawable) bg).findDrawableByLayerId(R.id.bg_inner_circle);
            Drawable outer = ((LayerDrawable) bg).findDrawableByLayerId(R.id.bg_outer_ring);
            if (inner instanceof GradientDrawable) {
                ((GradientDrawable) inner).setColor(color);
            }
            if (outer instanceof GradientDrawable) {
                int transparentColor = ColorUtils.setAlphaComponent(color, 80);
                ((GradientDrawable) outer).setColor(transparentColor);
            }
        }
    }

    private void tintUnselectedColorCircle(View view, int color) {
        view.setBackgroundResource(R.drawable.bg_color_circle);
        Drawable bg = view.getBackground();
        if (bg != null) bg.setTint(color);
    }
}


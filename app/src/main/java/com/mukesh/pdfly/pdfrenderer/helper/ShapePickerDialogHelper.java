package com.mukesh.pdfly.pdfrenderer.helper;

import android.app.Dialog;

import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.mukesh.pdfly.R;

public class ShapePickerDialogHelper {

    public interface ShapeSelectionListener {
        void onShapeSelected(PdfEditorActivity.ShapeType shapeType);
    }

    private final Activity activity;
    private final ShapeSelectionListener listener;

    public ShapePickerDialogHelper(Activity activity, ShapeSelectionListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void show() {
        Dialog dialog = new Dialog(activity);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_shape_picker, null);

        FrameLayout wrapper = new FrameLayout(activity);
        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, activity.getResources().getDisplayMetrics());
        wrapParams.setMargins(margin, margin, margin, margin);
        sheetView.setLayoutParams(wrapParams);
        sheetView.setBackgroundResource(R.drawable.floating_dialog_background);
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
            wlp.y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, activity.getResources().getDisplayMetrics());
            window.setAttributes(wlp);
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        // Set listeners for shape buttons
        sheetView.findViewById(R.id.btnRectangle).setOnClickListener(v -> {
            listener.onShapeSelected(PdfEditorActivity.ShapeType.RECTANGLE);
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.btnCircle).setOnClickListener(v -> {
            listener.onShapeSelected(PdfEditorActivity.ShapeType.CIRCLE);
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.btnArrow).setOnClickListener(v -> {
            listener.onShapeSelected(PdfEditorActivity.ShapeType.ARROW);
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.btnLine).setOnClickListener(v -> {
            listener.onShapeSelected(PdfEditorActivity.ShapeType.LINE);
            dialog.dismiss();
        });

        dialog.show();
    }
}


package com.mukesh.pdfly;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfEditorActivity extends AppCompatActivity implements DrawSettingsProvider{
    private int globalPaintColor = Color.BLACK;
    private float globalStrokeWidth = 1f;
    private ImageView drawToolIconView;  // Add this as a field


    private int currentDrawColor = Color.BLACK; // default

    private Uri pdfUri;
    private boolean isEditable;
    private LinearLayout pageContainer;
    private boolean isDrawMode = false;
    private ImageButton drawButton;
    private List<DrawView> drawViews = new ArrayList<>();

    private LinearLayout toolContainer;
    private int selectedToolIndex = -1;

    @Override
    public int getCurrentPaintColor() {
        return globalPaintColor;
    }

    @Override
    public float getCurrentStrokeWidth() {
        return globalStrokeWidth;
    }

    private enum ToolType {
        DRAW, UNDO, REDO, COLOR, SHAPE, SIGNATURE, COMMENT
    }
    private int[] toolIcons = {
            R.drawable.ic_pencil, R.drawable.ic_undo, R.drawable.ic_redo,
            R.drawable.ic_palette, R.drawable.ic_shape, R.drawable.ic_signature, R.drawable.ic_comment
    };

    private ToolType[] tools = {
            ToolType.DRAW, ToolType.UNDO, ToolType.REDO, ToolType.COLOR, ToolType.SHAPE, ToolType.SIGNATURE, ToolType.COMMENT
    };

    private void setupToolbar() {
        toolContainer = findViewById(R.id.toolContainer);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < tools.length; i++) {
            View toolItem = inflater.inflate(R.layout.item_tool_button, toolContainer, false);
            ImageView btnTool = toolItem.findViewById(R.id.btnTool);
            btnTool.setImageResource(toolIcons[i]);
            if (tools[i] == ToolType.DRAW) {
                drawToolIconView = btnTool; // store reference
            }
            int index = i;
            toolItem.setOnClickListener(v -> handleToolSelection(index));

            toolContainer.addView(toolItem);
        }
    }

    private void handleToolSelection(int index) {
        View tapped = toolContainer.getChildAt(index);

        // If the same tool is tapped again, popup if applicable
        if (selectedToolIndex == index) {
            if (!drawViews.isEmpty() && index==0) {
                showPenPopup();
                return;
            }
        }

        // Deselect previous
        if (selectedToolIndex != -1) {
            View prev = toolContainer.getChildAt(selectedToolIndex);
            prev.setSelected(false);
        }

        // Select new
        tapped.setSelected(true);
        selectedToolIndex = index;

        ToolType selectedTool = tools[index];
        switch (selectedTool) {
            case DRAW:
                isDrawMode = true;
                for (DrawView dv : drawViews) {
                    dv.setDrawingEnabled(true);
                }
                break;

            case COLOR:
                showColorPicker();
                break;
            case UNDO:
                for (DrawView dv : drawViews) {
                    if (dv.isShown()) {
                        dv.undo();
                    }
                }
                break;

            case REDO:
                for (DrawView dv : drawViews) {
                    if (dv.isShown()) {
                        dv.redo();
                    }
                }
                break;
            case SHAPE:
                isDrawMode = !isDrawMode;
                for (DrawView drawView : drawViews) {
                    drawView.setDrawingEnabled(isDrawMode); // ✅ ENABLES DRAWING!
                }
break;
            // Handle other tools...
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_editor);

        pdfUri = Uri.parse(getIntent().getStringExtra("pdfUri"));
        isEditable = getIntent().getBooleanExtra("isEditable", false);

        pageContainer = findViewById(R.id.pageContainer);
        drawButton = findViewById(R.id.drawButton);
        pageContainer.setBackgroundColor(Color.parseColor("#EEEEEE")); // light gray background

        drawButton.setOnClickListener(v -> {
            isDrawMode = !isDrawMode;

            // Toggle button UI
            if (isDrawMode) {
                drawButton.setBackgroundResource(R.drawable.bg_draw_button_active);
                Toast.makeText(this, "Drawing enabled", Toast.LENGTH_SHORT).show();
            } else {
                drawButton.setBackgroundResource(R.drawable.bg_draw_button_inactive);
                Toast.makeText(this, "Drawing disabled", Toast.LENGTH_SHORT).show();
            }

            // Update all DrawViews
            // Update all DrawViews
            for (DrawView drawView : drawViews) {
                drawView.setDrawingEnabled(isDrawMode); // ✅ ENABLES DRAWING!
            }

        });
        setupToolbar();
        renderAllPdfPages(pdfUri);
    }

//hihi
private void renderAllPdfPages(Uri uri) {
    try {
        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
        if (pfd != null) {
            PdfRenderer renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();

            int marginPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    12,
                    getResources().getDisplayMetrics()
            );

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                // Create Zoomable layout per page
                ZoomableFrameLayout zoomablePage = new ZoomableFrameLayout(this);
                LinearLayout.LayoutParams zoomParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                zoomParams.setMargins(0, marginPx, 0, marginPx); // Add gap between pages
                zoomablePage.setLayoutParams(zoomParams);
                zoomablePage.setBackgroundColor(Color.WHITE); // Each page is white

                // Add PDF image
                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                ));

                // Add DrawView overlay
                DrawView drawView = new DrawView(this);
                drawView.setDrawSettingsProvider(this);
                drawView.setDrawingEnabled(isDrawMode);
                drawView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

                // Combine views
                zoomablePage.addView(imageView);
                zoomablePage.addView(drawView);
                pageContainer.addView(zoomablePage);
                drawViews.add(drawView); // for undo/redo/toggle
            }

            renderer.close();
        }
    } catch (IOException e) {
        e.printStackTrace();
        Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show();
    }
}

    private void showColorPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_color_picker, null);
        LinearLayout container = view.findViewById(R.id.colorPickerContainer);

        int[] colors = {
                Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY
        };

        int size = (int) getResources().getDisplayMetrics().density * 48; // 48dp
        int margin = (int) getResources().getDisplayMetrics().density * 8;

        for (int color : colors) {
            View circle = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            circle.setLayoutParams(params);
            circle.setBackground(createCircleDrawable(color));

            circle.setOnClickListener(v -> {
                currentDrawColor = color;

//                // Update drawViews to use the new color
//                for (DrawView drawView : drawViews) {
//                    drawView.setPaintColor(currentDrawColor);
//                }

                dialog.dismiss();
            });

            container.addView(circle);
        }

        dialog.setContentView(view);
        dialog.show();
    }
    private Drawable createCircleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(100, 100); // size in pixels
        drawable.setStroke(3, Color.LTGRAY);
        return drawable;
    }
    private void showPenPopup() {
        Dialog dialog = new Dialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_pen_selector, null);

// Wrap sheetView to apply margins
        FrameLayout wrapper = new FrameLayout(this);
        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        wrapParams.setMargins(margin, margin, margin, margin);
        sheetView.setLayoutParams(wrapParams);
        wrapper.addView(sheetView);

        dialog.setContentView(wrapper);

// Now handle window params
        Window window = dialog.getWindow();
        if (window != null) {
            // Make background transparent to show rounded corners
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Remove background dim
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // Align to bottom with padding from bottom
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            wlp.gravity = Gravity.BOTTOM;
            wlp.y = 230; // Optional space from bottom if needed
            window.setAttributes(wlp);
        }


        // Stroke Size Logic
        TextView paintSize = sheetView.findViewById(R.id.paint_size);
        Slider strokeSlider = sheetView.findViewById(R.id.strokeSlider);

        paintSize.setText((int) globalStrokeWidth + "");
        strokeSlider.setValue(globalStrokeWidth);
        strokeSlider.addOnChangeListener((slider, value, fromUser) -> {
            globalStrokeWidth = value;
            paintSize.setText("" + (int) globalStrokeWidth);
        });

        // Color Picker Logic
        LinearLayout colorRow = sheetView.findViewById(R.id.colorRow);
        int[] colors = new int[]{
                Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA
        };

        final int[] selectedColor = {globalPaintColor};
        colorRow.removeAllViews();

        for (int color : colors) {
            View colorCircle = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(72, 72); // slightly bigger
            params.setMargins(16, 8, 16, 8);

            colorCircle.setLayoutParams(params);
            colorCircle.setTag(color);

            if (color == selectedColor[0]) {
                tintSelectedColorCircle(colorCircle, color);
            } else {
                tintUnselectedColorCircle(colorCircle, color);
            }

            colorCircle.setOnClickListener(v -> {
                selectedColor[0] = (int) v.getTag();
                globalPaintColor = selectedColor[0];
                updateDrawToolColorIndicator(globalPaintColor);

                // Update all color buttons
                for (int i = 0; i < colorRow.getChildCount(); i++) {
                    View c = colorRow.getChildAt(i);
                    int cColor = (int) c.getTag();
                    if (cColor == selectedColor[0]) {
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
    private void updateDrawToolColorIndicator(int color) {
        if (drawToolIconView != null && drawToolIconView.getDrawable() instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawToolIconView.getDrawable();
            Drawable colorLayer = layerDrawable.findDrawableByLayerId(R.id.color_line_layer);

            if (colorLayer instanceof GradientDrawable) {
                ((GradientDrawable) colorLayer).setColor(color);
            }
        }
    }
    private void tintSelectedColorCircle(View view, int color) {
        view.setBackgroundResource(R.drawable.bg_color_circle_selected);
        Drawable bg = view.getBackground();

        if (bg instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) bg;

            // Set inner solid circle (normal color)
            Drawable inner = layerDrawable.findDrawableByLayerId(R.id.bg_inner_circle);
            if (inner instanceof GradientDrawable) {
                ((GradientDrawable) inner).setColor(color);
            }

            // Set outer ring with lighter transparent color
            Drawable outer = layerDrawable.findDrawableByLayerId(R.id.bg_outer_ring);
            if (outer instanceof GradientDrawable) {
                int transparentColor = ColorUtils.setAlphaComponent(color, 80); // 80/255 ≈ 31% opacity
                ((GradientDrawable) outer).setColor(transparentColor);
            }
        }
    }
    private void tintUnselectedColorCircle(View view, int color) {
        view.setBackgroundResource(R.drawable.bg_color_circle);
        Drawable bg = view.getBackground();
        if (bg != null) {
            bg.setTint(color);
        }
    }



}

package com.mukesh.pdfly.pdfrenderer.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.Slider;
import com.mukesh.pdfly.DrawSettingsProvider;
import com.mukesh.pdfly.pdfrenderer.views.DrawView;
import com.mukesh.pdfly.R;
import com.mukesh.pdfly.pdfrenderer.views.ZoomableFrameLayout;
import com.mukesh.pdfly.signature.activity.SignatureCreatorActivity;
import com.mukesh.pdfly.signature.adapter.SignatureAdapter;
import com.mukesh.pdfly.signature.views.OverlayElementView;
import com.mukesh.pdfly.signature.views.SignatureElementView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class PdfEditorActivity extends AppCompatActivity implements DrawSettingsProvider {
    private int globalPaintColor = Color.BLACK;
    private float globalStrokeWidth = 1f;
    private ImageView drawToolIconView;
    private Bitmap selectedSignatureBitmap = null;

    private final List<ZoomableFrameLayout> zoomablePages = new ArrayList<>();
    private final List<DrawView> drawViews = new ArrayList<>();
    private final List<OverlayElementView> overlayElements = new ArrayList<>();

    private int currentDrawColor = Color.BLACK;
    private ShapeType selectedShapeType;

    public enum ShapeType {
        RECTANGLE, CIRCLE, ARROW, LINE
    }

    private Uri pdfUri;
    private boolean isEditable;
    private LinearLayout pageContainer;
    private boolean isDrawMode = false;
    private ImageButton drawButton;
    private LinearLayout toolContainer;
    private int selectedToolIndex = -1;
    private SignatureAdapter adapter;


    @Override
    public int getCurrentPaintColor() {
        return globalPaintColor;
    }

    @Override
    public float getCurrentStrokeWidth() {
        return globalStrokeWidth;
    }

    private enum ToolType {
        DRAW, UNDO, REDO, BLOCK_ACTION, SHAPE, SIGNATURE, COMMENT
    }

    private int[] toolIcons = {
            R.drawable.ic_pencil, R.drawable.ic_undo, R.drawable.ic_redo,
            R.drawable.ic_block_24dp, R.drawable.ic_shapes_24dp, R.drawable.ic_signature_solid, R.drawable.ic_comment
    };

    private ToolType[] tools = {
            ToolType.DRAW, ToolType.UNDO, ToolType.REDO, ToolType.BLOCK_ACTION, ToolType.SHAPE, ToolType.SIGNATURE, ToolType.COMMENT
    };

    private void setupToolbar() {
        toolContainer = findViewById(R.id.toolContainer);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < tools.length; i++) {
            View toolItem = inflater.inflate(R.layout.item_tool_button, toolContainer, false);
            ImageView btnTool = toolItem.findViewById(R.id.btnTool);
            btnTool.setImageResource(toolIcons[i]);
            if (tools[i] == ToolType.DRAW) {
                drawToolIconView = btnTool;
            }
            int index = i;
            toolItem.setOnClickListener(v -> handleToolSelection(index));
            toolContainer.addView(toolItem);
        }
    }

    private void handleToolSelection(int index) {
        View tapped = toolContainer.getChildAt(index);

        if (selectedToolIndex == index) {
            if (!drawViews.isEmpty() && index == 0) {
                showPenPopup();
                return;
            }
        }

        if (selectedToolIndex != -1 && !(index==1 || index ==2)) {
            View prev = toolContainer.getChildAt(selectedToolIndex);
            prev.setSelected(false);
        }
        if(!(index==1 || index ==2)){
            tapped.setSelected(true);
            selectedToolIndex = index;
        }


        ToolType selectedTool = tools[index];
        switch (selectedTool) {
            case DRAW:
                isDrawMode = true;
                for (DrawView dv : drawViews) {
                    dv.setDrawingEnabled(true);
                }
                break;
            case BLOCK_ACTION:
                isDrawMode = false;
                for (DrawView dv : drawViews) {
                    dv.setDrawingEnabled(isDrawMode);
                }
                selectedSignatureBitmap = null;
                break;
            case UNDO:
                if (!overlayElements.isEmpty()) {
                    OverlayElementView last = overlayElements.remove(overlayElements.size() - 1);
                    ((ViewGroup) last.getParent()).removeView(last);
                } else {
                    for (DrawView dv : drawViews) {
                        if (dv.isShown()) dv.undo();
                    }
                }
                break;
            case REDO:
                for (DrawView dv : drawViews) {
                    if (dv.isShown()) dv.redo();
                }
                break;
            case SHAPE:
                showShapePickerDialog();
                Toast.makeText(this, "Shape Clicked", Toast.LENGTH_SHORT).show();
                break;
            case SIGNATURE:
                isDrawMode = false;
                for (DrawView dv : drawViews) {
                    dv.setDrawingEnabled(isDrawMode);
                }
                showSignaturePickerDialog();
                break;
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
        pageContainer.setBackgroundColor(Color.parseColor("#EEEEEE"));

        drawButton.setOnClickListener(v -> {
            isDrawMode = !isDrawMode;
            drawButton.setBackgroundResource(isDrawMode ? R.drawable.bg_draw_button_active : R.drawable.bg_draw_button_inactive);
            for (DrawView dv : drawViews) {
                dv.setDrawingEnabled(isDrawMode);
            }
        });

        setupToolbar();
        renderAllPdfPages(pdfUri);
    }

    private void renderAllPdfPages(Uri uri) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) return;

            PdfRenderer renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();
            int marginPx = dpToPx(12);

            // Clear previous content if needed
            pageContainer.removeAllViews();
            zoomablePages.clear();
            drawViews.clear();
            overlayElements.clear();

            for (int i = 0; i < pageCount; i++) {
                // Render PDF page to bitmap
                PdfRenderer.Page page = renderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(
                        getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth(),
                        getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                // Create page container
                ZoomableFrameLayout zoomablePage = new ZoomableFrameLayout(this);
                zoomablePage.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ) {{
                    setMargins(0, marginPx, 0, marginPx);
                }});
                zoomablePage.setBackgroundColor(Color.WHITE);

                // Add PDF content image
                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

                // Add drawing layer
                DrawView drawView = new DrawView(this);
                drawView.setDrawSettingsProvider(this);
                drawView.setDrawingEnabled(isDrawMode);
                drawView.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));

                // Set up touch handling for signatures
                zoomablePage.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        // Check if we tapped on an existing overlay
                        OverlayElementView tappedOverlay = findOverlayAtPosition(zoomablePage, event.getX(), event.getY());

                        if (selectedOverlay != null) {
                            // If tapped outside current selection, deselect
                            if (tappedOverlay != selectedOverlay) {
                                deselectAllOverlays();
                            }
                            // If tapped on another overlay, select it
                            if (tappedOverlay != null && tappedOverlay != selectedOverlay) {
                                onElementSelected(tappedOverlay);
                            }
                            return true; // Consume the event
                        }

                        // If no overlay selected and tapped empty space with signature ready
                        if (tappedOverlay == null && selectedSignatureBitmap != null && !isDrawMode) {
                            addSignatureToPage(zoomablePage, event.getX(), event.getY());
                            return true;
                        }

                        // If tapped on an overlay with nothing selected, select it
                        if (tappedOverlay != null) {
                            onElementSelected(tappedOverlay);
                            return true;
                        }
                    }
                    return false;
                });

                // Add views to hierarchy
                zoomablePage.addView(imageView);
                zoomablePage.addView(drawView);
                pageContainer.addView(zoomablePage);

                // Track created views
                zoomablePages.add(zoomablePage);
                drawViews.add(drawView);
            }
            renderer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show();
        }
    }
    private OverlayElementView findOverlayAtPosition(ViewGroup parent, float x, float y) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof OverlayElementView) {
                if (isViewUnder(child, x, y)) {
                    return (OverlayElementView) child;
                }
            }
        }
        return null;
    }
    // Helper methods used in the main method
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
    private boolean isViewUnder(View view, float x, float y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Rect rect = new Rect(location[0], location[1],
                location[0] + view.getWidth(),
                location[1] + view.getHeight());

        int[] containerLocation = new int[2];
        pageContainer.getLocationOnScreen(containerLocation);
        float globalX = x + containerLocation[0];
        float globalY = y + containerLocation[1];

        return rect.contains((int)globalX, (int)globalY);
    }
    private boolean isViewUnder(View view, MotionEvent event) {
        if (view == null) return false;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Rect rect = new Rect(
                location[0],
                location[1],
                location[0] + view.getWidth(),
                location[1] + view.getHeight()
        );
        return rect.contains((int)event.getRawX(), (int)event.getRawY());
    }

    private void addSignatureToPage(ViewGroup parent, float x, float y) {
        // Scale down the bitmap for initial display
        int initialWidth = (int)(selectedSignatureBitmap.getWidth() * 0.5f); // 50% of original size
        int initialHeight = (int)(selectedSignatureBitmap.getHeight() * 0.5f);

        // Create a scaled version of the bitmap
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                selectedSignatureBitmap,
                initialWidth,
                initialHeight,
                true
        );

        // Create signature view with scaled bitmap
        SignatureElementView signatureView = new SignatureElementView(this, scaledBitmap);

        // Store original bitmap for later resizing if needed
        signatureView.setTag(selectedSignatureBitmap);

        // Calculate position (centered on touch point)
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = (int) x - (initialWidth / 2);
        params.topMargin = (int) y - (initialHeight / 2);

        signatureView.setLayoutParams(params);
        parent.addView(signatureView);
        overlayElements.add(signatureView);
        onElementSelected(signatureView);
    }
    private void showSignaturePickerDialog() {
        // Create dialog with transparent background
        Dialog dialog = new Dialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_signature_picker, null);

        // Apply margins and rounded corners
        FrameLayout wrapper = new FrameLayout(this);
        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        wrapParams.setMargins(margin, margin, margin, margin);
        sheetView.setLayoutParams(wrapParams);

        // Add card-like background
        sheetView.setBackgroundResource(R.drawable.floating_dialog_background);
        wrapper.addView(sheetView);
        dialog.setContentView(wrapper);

        // Window styling
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            wlp.gravity = Gravity.BOTTOM;
            wlp.y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
            window.setAttributes(wlp);

            // Add enter/exit animations
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        // Initialize views
        RecyclerView recyclerView = sheetView.findViewById(R.id.signatureRecyclerView);
        ImageButton btnAdd = sheetView.findViewById(R.id.btnAddSignature);

        // Load signatures
        List<File> signatures = loadSavedSignatureFiles();

        // Setup adapter
        adapter = new SignatureAdapter(signatures, selectedSignature -> {
            selectedSignatureBitmap = selectedSignature;
            dialog.dismiss();
        }, (position, file) -> {
            if (file.exists()) file.delete();
            adapter.removeAt(position);
            Toast.makeText(this, "Signature deleted", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            dialog.dismiss();
            openSignatureCreatorScreen();
        });

        dialog.show();
    }
    private List<File> loadSavedSignatureFiles() {
        List<File> result = new ArrayList<>();
        File dir = new File(getFilesDir(), "signatures");
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                result.add(file);
            }
        }
        return result;
    }

    private void showShapePickerDialog() {
        Dialog dialog = new Dialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_shape_picker, null);

        FrameLayout wrapper = new FrameLayout(this);
        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
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
            wlp.y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
            window.setAttributes(wlp);
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        ImageButton btnRectangle = sheetView.findViewById(R.id.btnRectangle);
        ImageButton btnCircle = sheetView.findViewById(R.id.btnCircle);
        ImageButton btnArrow = sheetView.findViewById(R.id.btnArrow);
        ImageButton btnLine = sheetView.findViewById(R.id.btnLine);

        View.OnClickListener shapeClickListener = v -> {
            int id = v.getId();
            if (id == R.id.btnRectangle) {
                selectedShapeType = ShapeType.RECTANGLE;
            } else if (id == R.id.btnCircle) {
                selectedShapeType = ShapeType.CIRCLE;
            } else if (id == R.id.btnArrow) {
                selectedShapeType = ShapeType.ARROW;
            } else if (id == R.id.btnLine) {
                selectedShapeType = ShapeType.LINE;
            }

            dialog.dismiss();
            //enableShapeDrawingMode(selectedShapeType);
        };

        btnRectangle.setOnClickListener(shapeClickListener);
        btnCircle.setOnClickListener(shapeClickListener);
        btnArrow.setOnClickListener(shapeClickListener);
        btnLine.setOnClickListener(shapeClickListener);

        dialog.show();
    }


    private static final int REQUEST_SIGNATURE_CREATE = 101;

    private void openSignatureCreatorScreen() {
        Intent intent = new Intent(this, SignatureCreatorActivity.class);
        startActivityForResult(intent, REQUEST_SIGNATURE_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGNATURE_CREATE && resultCode == RESULT_OK && data != null) {
            String path = data.getStringExtra("signaturePath");
            if (path != null) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                if (bmp != null) {
                    selectedSignatureBitmap = bmp;
                }
            }
        }
    }

    public void addSignatureToPage(Bitmap signatureBitmap, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= zoomablePages.size()) return;

        ZoomableFrameLayout page = zoomablePages.get(pageIndex);
        SignatureElementView signatureView = new SignatureElementView(this, signatureBitmap);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        signatureView.setLayoutParams(layoutParams);
        page.addView(signatureView);
        overlayElements.add(signatureView);
    }

    // Existing methods like showPenPopup(), showColorPicker(), etc. remain unchanged...
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
    private OverlayElementView selectedOverlay = null;

    public void onElementSelected(OverlayElementView element) {
        if (selectedOverlay != null && selectedOverlay != element) {
            selectedOverlay.setSelectedState(false);
        }

        selectedOverlay = element;
        selectedOverlay.setSelectedState(true);
    }
    public void deselectAllOverlays() {
        if (selectedOverlay != null) {
            selectedOverlay.setSelectedState(false);
            selectedOverlay = null;
        }
    }
    public void removeOverlayElement(OverlayElementView element) {
        for (ZoomableFrameLayout page : zoomablePages) {
            page.removeView(element);
        }
        overlayElements.remove(element);
    }


}

//
/*
public class PdfEditorActivity extends AppCompatActivity implements DrawSettingsProvider{
    private int globalPaintColor = Color.BLACK;
    private float globalStrokeWidth = 1f;
    private ImageView drawToolIconView;  // Add this as a field
    private Bitmap selectedSignatureBitmap = null;


    private final List<ZoomableFrameLayout> zoomablePages = new ArrayList<>();

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
            case SIGNATURE:
                showSignaturePickerBottomSheet();
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

                zoomablePages.add(zoomablePage);
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
    private void showSignaturePickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_signature_picker, null);
        RecyclerView recyclerView = view.findViewById(R.id.signatureRecyclerView);
        ImageButton btnAdd = view.findViewById(R.id.btnAddSignature);

        // Load signatures from local storage
        List<Bitmap> signatures = loadSavedSignatures();

        SignatureAdapter adapter = new SignatureAdapter(signatures, selectedSignature -> {
            selectedSignatureBitmap = selectedSignature;
            addSignatureToPage(selectedSignature,0);
            dialog.dismiss();
            // 🔥 Add to PDF now or pass to a handler
            //addSignatureToPdf(selectedSignature);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            dialog.dismiss();
            openSignatureCreatorScreen();
        });

        dialog.setContentView(view);
        dialog.show();
    }
    private List<Bitmap> loadSavedSignatures() {
        List<Bitmap> result = new ArrayList<>();
        File dir = new File(getFilesDir(), "signatures");

        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    result.add(bitmap);
                }
            }
        }

        return result;
    }

    private static final int REQUEST_SIGNATURE_CREATE = 101;

    private void openSignatureCreatorScreen() {
        Intent intent = new Intent(this, SignatureCreatorActivity.class);
        startActivityForResult(intent, REQUEST_SIGNATURE_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGNATURE_CREATE && resultCode == RESULT_OK && data != null) {
            String path = data.getStringExtra("signaturePath");
            if (path != null) {
                // Add this image as a movable/resizable signature on PDF
                //insertSignatureFromPath(path);
            }
        }
    }

    public void addSignatureToPage(Bitmap signatureBitmap, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= zoomablePages.size()) return;

        ZoomableFrameLayout page = zoomablePages.get(pageIndex);
        SignatureElementView signatureView = new SignatureElementView(this, signatureBitmap);

        // Add to center initially
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        signatureView.setLayoutParams(layoutParams);

        page.addView(signatureView);
    }


}
*/

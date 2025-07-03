package com.mukesh.pdfly.pdfrenderer.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mukesh.pdfly.DrawSettingsProvider;
import com.mukesh.pdfly.pdfrenderer.helper.PdfRendererHelper;
import com.mukesh.pdfly.pdfrenderer.helper.PenSettingsDialogHelper;
import com.mukesh.pdfly.pdfrenderer.helper.ShapeElementView;
import com.mukesh.pdfly.pdfrenderer.helper.ShapePickerDialogHelper;
import com.mukesh.pdfly.pdfrenderer.helper.TextElementView;
import com.mukesh.pdfly.pdfrenderer.helper.ToolManager;
import com.mukesh.pdfly.pdfrenderer.views.DrawView;
import com.mukesh.pdfly.R;
import com.mukesh.pdfly.pdfrenderer.views.ZoomableFrameLayout;
import com.mukesh.pdfly.signature.activity.SignatureCreatorActivity;
import com.mukesh.pdfly.signature.adapter.SignatureAdapter;
import com.mukesh.pdfly.signature.helper.SignaturePickerDialogHelper;
import com.mukesh.pdfly.pdfrenderer.helper.CheckmarkElementView;
import com.mukesh.pdfly.signature.views.OverlayElementView;
import com.mukesh.pdfly.signature.views.SignatureElementView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class PdfEditorActivity extends AppCompatActivity implements DrawSettingsProvider {
    private int globalPaintColor = Color.BLACK;
    private float globalStrokeWidth = 9f;
    private ImageView drawToolIconView;
    private Bitmap selectedSignatureBitmap = null;

    private final List<ZoomableFrameLayout> zoomablePages = new ArrayList<>();
    private final List<DrawView> drawViews = new ArrayList<>();
    private final List<OverlayElementView> overlayElements = new ArrayList<>();

    private ToolManager toolManager;

    private SignaturePickerDialogHelper signaturePickerDialogHelper;

    private Uri pdfUri;
    private boolean isEditable;
    private LinearLayout pageContainer;
    private boolean isDrawMode = false;
    private ImageButton drawButton;
    private int selectedToolIndex = -1;

    private ShapeType selectedShapeType = null;

    private SignatureAdapter adapter;
    private PenSettingsDialogHelper penSettingsDialogHelper;

    private ShapePickerDialogHelper shapePickerDialogHelper;
    private boolean isCheckmarkSelected = false;
    private boolean blockAllActions = false;
    private boolean selectedTextMode = false; // set this true when text icon is clicked


    public enum ShapeType {
        RECTANGLE, CIRCLE, ARROW, LINE
    }

    @Override
    public int getCurrentPaintColor() {
        return globalPaintColor;
    }

    @Override
    public float getCurrentStrokeWidth() {
        return globalStrokeWidth;
    }

    private void handleToolSelection(ToolManager.ToolType selectedTool, int index) {
        selectedTextMode = false;
        blockAllActions = false;
        deselectAllOverlays();
        switch (selectedTool) {
            case DRAW:
                if (selectedToolIndex == index) {
                    penSettingsDialogHelper.show();
                    // Reopen if tapped again
                    return;
                }
                isDrawMode = true;
                for (DrawView dv : drawViews) {
                    dv.setDrawingEnabled(true);
                }
                break;

            case BLOCK_ACTION:
                blockAllActions = true;
                isDrawMode = false;
                for (DrawView dv : drawViews) {
                    dv.setDrawingEnabled(false);
                }
                selectedSignatureBitmap = null;
                break;

            case UNDO:
                cleanOrphanOverlays();
                if (!overlayElements.isEmpty()) {
                    OverlayElementView last = overlayElements.get(overlayElements.size() - 1);
                    ViewGroup parent = (ViewGroup) last.getParent();
                    if (parent != null) {
                        parent.removeView(last);
                    }
                    overlayElements.remove(overlayElements.size() - 1);
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
                deselectAllOverlays();
                isDrawMode = false;
                for (DrawView dv : drawViews) dv.setDrawingEnabled(false);
                shapePickerDialogHelper.show();
                break;
            case SIGNATURE:
                isDrawMode = false;
                for (DrawView dv : drawViews) {
                    dv.setDrawingEnabled(false);
                }
                signaturePickerDialogHelper.show();
                break;
            case COMMENT:
                deselectAllOverlays();
                isCheckmarkSelected = true;
                selectedShapeType = null;
                selectedSignatureBitmap = null;
                isDrawMode = false;
                for (DrawView dv : drawViews) dv.setDrawingEnabled(false);

                Toast.makeText(this, "Comment Tool (not implemented)", Toast.LENGTH_SHORT).show();
                break;
            case TEXT:
                deselectAllOverlays();
                selectedTextMode = true;
                isCheckmarkSelected = false;
                selectedShapeType = null;
                selectedSignatureBitmap = null;
                isDrawMode = false;
                for (DrawView dv : drawViews) dv.setDrawingEnabled(false);

                Toast.makeText(this, "Comment Tool (not implemented)", Toast.LENGTH_SHORT).show();
                break;
        }
        selectedToolIndex = index;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_editor);

        toolManager = new ToolManager(this, findViewById(R.id.toolContainer), this::handleToolSelection);
        drawToolIconView = toolManager.getDrawToolIconView();

        signaturePickerDialogHelper = new SignaturePickerDialogHelper(this, new SignaturePickerDialogHelper.SignatureSelectionListener() {
            @Override
            public void onSignatureSelected(Bitmap signature) {
                selectedSignatureBitmap = signature;
            }

            @Override
            public void onAddNewSignatureRequested() {
                openSignatureCreatorScreen();
            }
        });

        shapePickerDialogHelper = new ShapePickerDialogHelper(this, shapeType -> {
            selectedShapeType = shapeType;
            Toast.makeText(this, "Shape: " + shapeType.name(), Toast.LENGTH_SHORT).show();
        });

        penSettingsDialogHelper = new PenSettingsDialogHelper(this, globalPaintColor, globalStrokeWidth, new PenSettingsDialogHelper.OnPenSettingsChangedListener() {
            @Override
            public void onColorChanged(int color) {
                globalPaintColor = color;
                updateDrawToolColorIndicator(color);
            }

            @Override
            public void onStrokeWidthChanged(float stroke) {
                globalStrokeWidth = stroke;
            }
        });

//        pdfUri = Uri.parse(getIntent().getStringExtra("pdfUri"));
//        isEditable = getIntent().getBooleanExtra("isEditable", false);
        Intent intent = getIntent();
        Uri pdfUri = intent.getData();

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
        if (pdfUri != null) {
            renderAllPdfPages(pdfUri);
        } else {
            Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show();
            finish();
        }
       // renderAllPdfPages(pdfUri);
    }
    private void addShapeToPage(ViewGroup parent, float x, float y, ShapeType shapeType) {
        ShapeElementView shapeView = new ShapeElementView(this, shapeType, globalPaintColor, globalStrokeWidth);

        int width = dpToPx(100);
        int height = dpToPx(100);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.leftMargin = (int) x - width / 2;
        params.topMargin = (int) y - height / 2;

        shapeView.setLayoutParams(params);
        parent.addView(shapeView);
        overlayElements.add(shapeView);
        onElementSelected(shapeView);
    }

    private void renderAllPdfPages(Uri uri) {
        int marginPx = dpToPx(12);

        pageContainer.removeAllViews();
        pageContainer.removeAllViews();
        zoomablePages.clear();
        drawViews.clear();
        overlayElements.clear();

        PdfRendererHelper.renderAllPages(this, uri, (bitmap, index) -> {
            ZoomableFrameLayout zoomablePage = new ZoomableFrameLayout(this);
            zoomablePage.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ) {{
                setMargins(0, marginPx, 0, marginPx);
            }});
            zoomablePage.setBackgroundColor(Color.WHITE);

            // PDF content image
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setAdjustViewBounds(true);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // Drawing overlay
            DrawView drawView = new DrawView(this);
            drawView.setDrawSettingsProvider(this);
            drawView.setDrawingEnabled(isDrawMode);
            drawView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

                // Set up touch handling for signatures
            zoomablePage.setOnTouchListener((v, event) -> {
                if(blockAllActions){
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    OverlayElementView tappedOverlay = findOverlayAtPosition(zoomablePage, event.getX(), event.getY());

                    // Case 1: If any overlay is selected
                    if (selectedOverlay != null) {
                        if (tappedOverlay != selectedOverlay) {
                            // Tapped somewhere else — either empty space or another overlay
                            deselectAllOverlays();
                        }

                        if (tappedOverlay != null && tappedOverlay != selectedOverlay) {
                            onElementSelected(tappedOverlay); // Select new
                        }

                        return true;
                    }

                    // Case 2: Tap on overlay when none was selected
                    if (tappedOverlay != null) {
                        onElementSelected(tappedOverlay);
                        return true;
                    }

                    // Case 3: Tap on empty space with insertion tools
                    if (tappedOverlay == null) {
                        if (selectedTextMode && !isDrawMode) {
                            addTextToPage(zoomablePage, event.getX(), event.getY());
                            return true;
                        }

                        // Other cases (signatures, checkmarks, shapes)
                    }

                    if (tappedOverlay == null) {
                        // Signature mode
                        if (selectedSignatureBitmap != null && !isDrawMode) {
                            addSignatureToPage(zoomablePage, event.getX(), event.getY());
                            return true;
                        }

                        // Checkmark mode
                        if (isCheckmarkSelected && !isDrawMode) {
                            addCheckmarkToPage(zoomablePage, event.getX(), event.getY());
                            return true;
                        }

                        // Shape mode
                        if (selectedShapeType != null && !isDrawMode) {
                            addShapeToPage(zoomablePage, event.getX(), event.getY(), selectedShapeType);
                            return true;
                        }

                        // 💡 This is the missing part:
                        // If we tapped on empty space and nothing is being added, just deselect anything selected
                        deselectAllOverlays();
                        return true;
                    }
                }
                return false;
            });

            zoomablePage.addView(imageView);
            zoomablePage.addView(drawView);
            zoomablePage.setClipChildren(true);
            zoomablePage.setClipToPadding(true);
            pageContainer.addView(zoomablePage);

            zoomablePages.add(zoomablePage);
            drawViews.add(drawView);
        });
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
    private void cleanOrphanOverlays() {
        Iterator<OverlayElementView> iterator = overlayElements.iterator();
        while (iterator.hasNext()) {
            OverlayElementView v = iterator.next();
            if (v.getParent() == null) {
                iterator.remove();
            }
        }
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
    private void updateDrawToolColorIndicator(int color) {
        if (drawToolIconView != null && drawToolIconView.getDrawable() instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawToolIconView.getDrawable();
            Drawable colorLayer = layerDrawable.findDrawableByLayerId(R.id.color_line_layer);

            if (colorLayer instanceof GradientDrawable) {
                ((GradientDrawable) colorLayer).setColor(color);
            }
        }
    }

    //signature overlay methods
    private OverlayElementView selectedOverlay = null;

    public void onElementSelected(OverlayElementView element) {
        if (selectedOverlay != null && selectedOverlay != element) {
            selectedOverlay.setSelectedState(false);
            hideElementToolbar();
        }

        selectedOverlay = element;
        selectedOverlay.setSelectedState(true);
    }
    public void deselectAllOverlays() {
        if (selectedOverlay != null) {
            selectedOverlay.setSelectedState(false);
            selectedOverlay = null;
        }
        hideElementToolbar();
    }
    public void removeOverlayElement(OverlayElementView element) {
        for (ZoomableFrameLayout page : zoomablePages) {
            page.removeView(element);
        }
        overlayElements.remove(element);
        hideElementToolbar();
    }
    private OverlayElementView findOverlayAtPosition(ViewGroup parent, float x, float y) {
        int[] containerLocation = new int[2];
        parent.getLocationOnScreen(containerLocation);
        float globalX = x + containerLocation[0];
        float globalY = y + containerLocation[1];

        for (int i = parent.getChildCount() - 1; i >= 0; i--) { // Topmost view first
            View child = parent.getChildAt(i);
            if (child instanceof OverlayElementView) {
                OverlayElementView overlay = (OverlayElementView) child;
                if (overlay.hitTest(globalX, globalY)) {
                    return overlay;
                }
            }
        }
        return null;
    }


    public void showShapeContextMenu(ShapeElementView shapeView) {
        PopupMenu popup = new PopupMenu(this, shapeView); // or use an anchor view
        popup.getMenuInflater().inflate(R.menu.shape_context_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if(id ==R.id.menu_change_color ) {
                openColorPickerDialog(shapeView);
                return true;
            }

            if(id ==R.id.menu_change_stroke ) {
                openStrokeWidthDialog(shapeView);
                return true;
            }
            return false;

        });

        popup.show();
    }

    private void openColorPickerDialog(ShapeElementView shapeView) {
    }

    private void openStrokeWidthDialog(ShapeElementView shape) {
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(50);
        seekBar.setProgress((int) shape.getStrokeWidth());

        new AlertDialog.Builder(this)
                .setTitle("Select Stroke Width")
                .setView(seekBar)
                .setPositiveButton("OK", (dialog, which) -> {
                    shape.setStrokeWidth(seekBar.getProgress());
                })
                .show();
    }
    public void showShapeToolbar(final OverlayElementView elementView) {
        FrameLayout overlay = findViewById(R.id.overlay_container);

        // Remove existing toolbar
        overlay.removeView(findViewById(R.id.element_toolbar));

        View toolbar = LayoutInflater.from(this).inflate(R.layout.view_element_toolbar, overlay, false);

        // Set visibility based on element type
//        toolbar.findViewById(R.id.btn_text_size).setVisibility(
//                elementView instanceof TextElementView ? View.VISIBLE : View.GONE
//        );

        // Setup listeners
        toolbar.findViewById(R.id.btn_color).setOnClickListener(v -> {
            showColorPicker(elementView);
        });

        toolbar.findViewById(R.id.btn_stroke).setOnClickListener(v -> {
            openStrokeWidthDialog((ShapeElementView) elementView);
        });

        toolbar.findViewById(R.id.btn_text_size).setOnClickListener(v -> {
            //showTextSizeDialog((TextElementView) elementView);
        });

        // Position toolbar near element
        int[] location = new int[2];
        elementView.getLocationOnScreen(location);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        lp.leftMargin = location[0];
        lp.topMargin = location[1] - dpToPx(100); // Slightly above element
        toolbar.setLayoutParams(lp);

        overlay.addView(toolbar);
    }
    public void hideElementToolbar() {
        FrameLayout overlay = findViewById(R.id.overlay_container);
        View toolbar = overlay.findViewById(R.id.element_toolbar);
        if (toolbar != null) {
            overlay.removeView(toolbar);
        }
    }
    private void showColorPicker(OverlayElementView shape) {
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
                shape.setColor(color);

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
    private void addCheckmarkToPage(ViewGroup page, float x, float y) {
        CheckmarkElementView checkmark = new CheckmarkElementView(this);
        int size = dpToPx(100);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        checkmark.setLayoutParams(lp);

        checkmark.setX(x - size / 2f); // Center it on touch
        checkmark.setY(y - size / 2f);

        page.addView(checkmark);
        overlayElements.add(checkmark); // For undo/selection tracking
        onElementSelected(checkmark);
    }

    public void addTextToPage(ViewGroup page, float x, float y) {
        String initialText = "Enter text"; // Or insert formatted date if date mode
        int defaultColor = Color.BLACK;
        float defaultSizeSp = 18f;

        TextElementView textElement = new TextElementView(this, initialText, defaultColor, defaultSizeSp);

        int initialWidth = dpToPx(150);
        int initialHeight = dpToPx(60);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(initialWidth, initialHeight);
        textElement.setLayoutParams(layoutParams);

        page.addView(textElement);

        // Offset to center the touch point
        textElement.setX(x - initialWidth / 2f);
        textElement.setY(y - initialHeight / 2f);

        textElement.setSelectedState(true);
        onElementSelected(textElement);
        showTextEditDialog(textElement);
    }
    private void showTextEditDialog(TextElementView textView) {
        EditText editText = new EditText(this);
        editText.setText(textView.getText().toString());

        new AlertDialog.Builder(this)
                .setTitle("Edit Text")
                .setView(editText)
                .setPositiveButton("OK", (dialog, which) -> {
                    textView.setText(editText.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}

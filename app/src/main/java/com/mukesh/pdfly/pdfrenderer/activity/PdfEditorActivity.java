package com.mukesh.pdfly.pdfrenderer.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mukesh.pdfly.DrawSettingsProvider;
import com.mukesh.pdfly.databinding.ViewElementToolbarBinding;
import com.mukesh.pdfly.pdfrenderer.helper.PdfRendererHelper;
import com.mukesh.pdfly.pdfrenderer.helper.PenSettingsDialogHelper;
import com.mukesh.pdfly.pdfrenderer.views.ShapeElementView;
import com.mukesh.pdfly.pdfrenderer.helper.ShapePickerDialogHelper;
import com.mukesh.pdfly.pdfrenderer.views.TextElementView;
import com.mukesh.pdfly.pdfrenderer.helper.ToolManager;
import com.mukesh.pdfly.pdfrenderer.views.DrawView;
import com.mukesh.pdfly.R;
import com.mukesh.pdfly.pdfrenderer.views.ZoomableFrameLayout;
import com.mukesh.pdfly.signature.activity.SignatureCreatorActivity;
import com.mukesh.pdfly.signature.adapter.SignatureAdapter;
import com.mukesh.pdfly.signature.helper.SignaturePickerDialogHelper;
import com.mukesh.pdfly.pdfrenderer.views.CheckmarkElementView;
import com.mukesh.pdfly.signature.views.OverlayElementView;
import com.mukesh.pdfly.signature.views.SignatureElementView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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
    private boolean selectedDateMode = false; // set this true when text icon is clicked
    //to move toolbar options with shape
    private View elementToolbar;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        savePdfWithOverlays();
    }


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
                selectedShapeType = null;
                selectedSignatureBitmap = null;
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
                selectedDateMode = false;
                selectedTextMode = true;
                isCheckmarkSelected = false;
                selectedShapeType = null;
                selectedSignatureBitmap = null;
                isDrawMode = false;
                for (DrawView dv : drawViews) dv.setDrawingEnabled(false);

                Toast.makeText(this, "Comment Tool (not implemented)", Toast.LENGTH_SHORT).show();
                break;
            case DATE:
                deselectAllOverlays();
                selectedDateMode = true;
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
        View existingToolbar = findViewById(R.id.element_toolbar);
        if (existingToolbar != null) {
            overlay.removeView(existingToolbar);
        }

        // Inflate using View Binding
        ViewElementToolbarBinding binding = ViewElementToolbarBinding.inflate(
                LayoutInflater.from(this),
                overlay,
                false
        );
        elementToolbar = binding.getRoot();

        // Set visibility based on element type
        binding.btnTextSizeInc.setVisibility(
                elementView instanceof TextElementView ? View.VISIBLE : View.GONE
        );
        binding.btnTextSizeDec.setVisibility(
                elementView instanceof TextElementView ? View.VISIBLE : View.GONE
        );
        binding.btnTextBold.setVisibility(
                elementView instanceof TextElementView ? View.VISIBLE : View.GONE
        );
        binding.btnFill.setVisibility(
                elementView instanceof TextElementView ? View.VISIBLE : View.GONE
        );
        binding.btnStroke.setVisibility(
                elementView instanceof ShapeElementView ? View.VISIBLE : View.GONE
        );
        binding.btnEditTxt.setVisibility(
                elementView instanceof TextElementView ? View.VISIBLE : View.GONE
        );

        // Setup listeners
        binding.btnColor.setOnClickListener(v -> showColorPicker(elementView));

        if (elementView instanceof ShapeElementView) {
            binding.btnStroke.setOnClickListener(v ->
                    openStrokeWidthDialog((ShapeElementView) elementView));
        }

        if(elementView instanceof TextElementView){
            binding.btnTextBold.setSelected(((TextElementView) elementView).isBold());

            binding.btnEditTxt.setOnClickListener(v -> showTextEditDialog((TextElementView) elementView));
            binding.btnFill.setOnClickListener(v -> showColorPicker(elementView));
            binding.btnTextSizeInc.setOnClickListener(v -> elementView.increaseSize());
            binding.btnTextSizeDec.setOnClickListener(v -> elementView.decreaseSize());
            binding.btnTextBold.setOnClickListener(v -> {
                if (elementView instanceof TextElementView) {
                    TextElementView textElement = (TextElementView) elementView;
                    boolean currentBoldState = textElement.isBold();
                    textElement.setBold(!currentBoldState);
                    binding.btnTextBold.setSelected(!currentBoldState);
                } else {
                    // Optionally show a toast or handle the error case
                    Toast.makeText(this, "Bold only applies to text elements", Toast.LENGTH_SHORT).show();
                }
            });
        }


        // Position toolbar near element
        int[] location = new int[2];
        elementView.getLocationOnScreen(location);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        lp.leftMargin = location[0];
        lp.topMargin = location[1] - dpToPx(100); // Slightly above element
        binding.getRoot().setLayoutParams(lp);

        // Store reference to binding if needed later
        binding.getRoot().setTag(R.id.element_toolbar, binding);
        overlay.addView(binding.getRoot());
    }    public void updateToolbarPosition(OverlayElementView elementView) {
        if (elementToolbar == null) return;

        int[] location = new int[2];
        elementView.getLocationOnScreen(location);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) elementToolbar.getLayoutParams();
        lp.leftMargin = location[0];
        lp.topMargin = location[1] - dpToPx(100); // Adjust as needed
        elementToolbar.setLayoutParams(lp);
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

        // Use HorizontalScrollView for horizontal scrolling if needed
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(container);

        int[] colors = {
                Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY,
                Color.LTGRAY, Color.DKGRAY, Color.WHITE, Color.TRANSPARENT,
                0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, // Material colors
                0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
                0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
                0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722
        };

        // Calculate dynamic sizing
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int colorCount = colors.length;
        int minItemSize = dpToPx(48); // Minimum 48dp per color
        int totalMarginSpace = dpToPx(16) * (colorCount + 1); // 8dp margin on each side

        // Calculate equal spacing
        int itemSize = Math.min(
                minItemSize,
                (screenWidth - totalMarginSpace) / colorCount
        );

        int margin = (screenWidth - (itemSize * colorCount)) / (colorCount + 1);

        for (int color : colors) {
            View circle = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    itemSize,
                    itemSize
            );
            params.setMargins(margin, dpToPx(16), 0, dpToPx(16));

            circle.setLayoutParams(params);
            circle.setBackground(createCircleDrawable(color));
            circle.setOnClickListener(v -> {
                if (shape instanceof TextElementView) {
                    ((TextElementView) shape).setBackgroundColorInt(color);
                } else {
                    shape.setColor(color);
                }
                dialog.dismiss();
            });

            container.addView(circle);
        }

        dialog.setContentView(scrollView);
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
        overlayElements.add(textElement);
        textElement.setSelectedState(true);
        onElementSelected(textElement);
        if(!selectedDateMode)
        showTextEditDialog(textElement);
        else{
            showDatePickerForTextElement(textElement);
        }
    }
    private void showTextEditDialog(TextElementView textView) {
        // Create Material text input layout
        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.setPadding(
                dpToPx(0), // left
                dpToPx(8),  // top
                dpToPx(16), // right
                dpToPx(8)   // bottom
        );

        // Create EditText with Material style
        TextInputEditText editText = new TextInputEditText(this);
        editText.setText(textView.getText().toString());
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editText.setPadding(
                dpToPx(12), // left
                dpToPx(16), // top
                dpToPx(12), // right
                dpToPx(16)  // bottom
        );
        editText.setSingleLine(false);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setBackground(null); // Remove default background

        // Set layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(24), 0, dpToPx(24), 0);
        editText.setLayoutParams(params);

        textInputLayout.addView(editText);

        // Create Material dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Text")
                .setView(textInputLayout)
                //.setBackground(getResources().getDrawable(R.drawable.bg_border_shadow, null))
                .setPositiveButton("OK", (dialog, which) -> {
                    textView.setText(editText.getText().toString());
                })
                .setNegativeButton("CANCEL", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Style the buttons
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null && negativeButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.color_orange));
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.color_orange));
        }

        // Show keyboard automatically
        editText.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void showDatePickerForTextElement(TextElementView element) {
        // Use MaterialDatePicker from Material Components
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convert milliseconds to readable date format
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);

            // Format date (using localized format)
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String formattedDate = sdf.format(calendar.getTime());

            element.setText(formattedDate);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }


    private void savePdfWithOverlayss() {
        // Create a temporary directory to store processed pages
        File outputDir = new File(getCacheDir(), "pdf_export");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Create a progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Process in background thread
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... voids) {
                try {
                    // Create a new PDF document
                    PdfDocument document = new PdfDocument();
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

                    // Process each page
                    for (int i = 0; i < zoomablePages.size(); i++) {
                        ZoomableFrameLayout pageLayout = zoomablePages.get(i);
                        DrawView drawView = drawViews.get(i);

                        // Get the original PDF page size
                        ImageView pdfImageView = (ImageView) pageLayout.getChildAt(0);
                        Bitmap originalBitmap = ((BitmapDrawable) pdfImageView.getDrawable()).getBitmap();
                        int pageWidth = originalBitmap.getWidth();
                        int pageHeight = originalBitmap.getHeight();

                        // Create a bitmap for the combined content
                        Bitmap combinedBitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(combinedBitmap);

                        // 1. Draw the original PDF content
                        canvas.drawBitmap(originalBitmap, 0, 0, null);

                        // 2. Draw the drawing layer
                        Bitmap drawingBitmap = drawView.getBitmap();
                        if (drawingBitmap != null) {
                            // Scale drawing to match PDF size
                            Matrix matrix = new Matrix();
                            float scaleX = (float) pageWidth / drawView.getWidth();
                            float scaleY = (float) pageHeight / drawView.getHeight();
                            matrix.postScale(scaleX, scaleY);
                            canvas.drawBitmap(drawingBitmap, matrix, null);
                        }

                        // 3. Draw all overlay elements
                        for (int j = 0; j < pageLayout.getChildCount(); j++) {
                            View child = pageLayout.getChildAt(j);
                            if (child instanceof OverlayElementView) {
                                OverlayElementView overlay = (OverlayElementView) child;
                                Bitmap overlayBitmap = overlay.getBitmap();

                                if (overlayBitmap != null) {
                                    // Calculate position relative to PDF size
                                    float left = overlay.getX() * ((float) pageWidth / pageLayout.getWidth());
                                    float top = overlay.getY() * ((float) pageHeight / pageLayout.getHeight());
                                    float width = overlay.getWidth() * ((float) pageWidth / pageLayout.getWidth());
                                    float height = overlay.getHeight() * ((float) pageHeight / pageLayout.getHeight());

                                    // Scale the overlay bitmap
                                    Bitmap scaledOverlay = Bitmap.createScaledBitmap(overlayBitmap, (int) width, (int) height, true);
                                    canvas.drawBitmap(scaledOverlay, left, top, null);
                                }
                            }
                        }

                        // Create a PDF page
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i).create();
                        PdfDocument.Page page = document.startPage(pageInfo);

                        // Draw the combined bitmap to the PDF page
                        page.getCanvas().drawBitmap(combinedBitmap, 0, 0, null);
                        document.finishPage(page);

                        // Recycle bitmaps
                        combinedBitmap.recycle();
                    }

                    // Save the document to a file
                    File outputFile = new File(outputDir, "annotated_" + System.currentTimeMillis() + ".pdf");
                    document.writeTo(new FileOutputStream(outputFile));
                    document.close();

                    return outputFile;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(File result) {
                if (!isFinishing() && !isDestroyed()) {
                    progressDialog.dismiss();
                }
                if (result != null) {
                    // Share the saved PDF
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    Uri uri = FileProvider.getUriForFile(PdfEditorActivity.this,
                            getPackageName() + ".fileprovider", result);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(shareIntent, "Share PDF"));
                } else {
                    Toast.makeText(PdfEditorActivity.this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void savePdfWithOverlays() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... voids) {
                try {
                    // Use Downloads directory
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }

                    File outputFile = new File(downloadsDir, "annotated_" + System.currentTimeMillis() + ".pdf");

                    // Create a new PDF document
                    PdfDocument document = new PdfDocument();

                    for (int i = 0; i < zoomablePages.size(); i++) {
                        ZoomableFrameLayout pageLayout = zoomablePages.get(i);
                        DrawView drawView = drawViews.get(i);

                        // Get original page size
                        ImageView pdfImageView = (ImageView) pageLayout.getChildAt(0);
                        Bitmap originalBitmap = ((BitmapDrawable) pdfImageView.getDrawable()).getBitmap();
                        int pageWidth = originalBitmap.getWidth();
                        int pageHeight = originalBitmap.getHeight();

                        // Create combined bitmap
                        Bitmap combinedBitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(combinedBitmap);

                        // Draw original PDF
                        canvas.drawBitmap(originalBitmap, 0, 0, null);

                        // Draw drawing layer
                        Bitmap drawingBitmap = drawView.getBitmap();
                        if (drawingBitmap != null) {
                            Matrix matrix = new Matrix();
                            float scaleX = (float) pageWidth / drawView.getWidth();
                            float scaleY = (float) pageHeight / drawView.getHeight();
                            matrix.postScale(scaleX, scaleY);
                            canvas.drawBitmap(drawingBitmap, matrix, null);
                        }

                        // Draw overlays
                        for (int j = 0; j < pageLayout.getChildCount(); j++) {
                            View child = pageLayout.getChildAt(j);
                            if (child instanceof OverlayElementView) {
                                OverlayElementView overlay = (OverlayElementView) child;
                                Bitmap overlayBitmap = overlay.getBitmap();
                                if (overlayBitmap != null) {
                                    float left = overlay.getX() * ((float) pageWidth / pageLayout.getWidth());
                                    float top = overlay.getY() * ((float) pageHeight / pageLayout.getHeight());
                                    float width = overlay.getWidth() * ((float) pageWidth / pageLayout.getWidth());
                                    float height = overlay.getHeight() * ((float) pageHeight / pageLayout.getHeight());
                                    Bitmap scaledOverlay = Bitmap.createScaledBitmap(overlayBitmap, (int) width, (int) height, true);
                                    canvas.drawBitmap(scaledOverlay, left, top, null);
                                }
                            }
                        }

                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i).create();
                        PdfDocument.Page page = document.startPage(pageInfo);
                        page.getCanvas().drawBitmap(combinedBitmap, 0, 0, null);
                        document.finishPage(page);

                        combinedBitmap.recycle();
                    }

                    // Save PDF to Downloads
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    document.writeTo(fos);
                    document.close();
                    fos.close();

                    return outputFile;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(File result) {
                if (!isFinishing() && !isDestroyed()) {
                    progressDialog.dismiss();
                }

                if (result != null) {
                    Toast.makeText(PdfEditorActivity.this, "Saved to Downloads", Toast.LENGTH_SHORT).show();

                    // Optional: Trigger media scan so it's visible in file managers
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(result));
                    sendBroadcast(scanIntent);
                } else {
                    Toast.makeText(PdfEditorActivity.this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }


}

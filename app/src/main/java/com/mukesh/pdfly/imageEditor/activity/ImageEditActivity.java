package com.mukesh.pdfly.imageEditor.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.mukesh.pdfly.imageEditor.views.CropOverlayView;
import com.mukesh.pdfly.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageEditActivity extends AppCompatActivity {

    private static final String TAG = "ImageEditActivity";

    // Intent extras keys for passing data
    public static final String EXTRA_IMAGE_URI = "image_uri"; // Input URI from calling Activity
    public static final String EXTRA_RESULT_URI = "result_uri"; // Output URI to calling Activity

    private ImageView imageView;
    private CropOverlayView cropOverlayView;
    private Uri originalImageUri;
    private Bitmap originalBitmap; // The loaded bitmap, which will be rotated
    private int currentRotationDegrees = 0; // Tracks the total rotation applied to the bitmap

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to your activity_image_edit.xml layout
        setContentView(R.layout.activity_image_edit);

        // Initialize UI elements
        imageView = findViewById(R.id.image_view);
        cropOverlayView = findViewById(R.id.crop_overlay_view);

        // Set up button click listeners
        Button btnRotateLeft = findViewById(R.id.btn_rotate_left);
        Button btnRotateRight = findViewById(R.id.btn_rotate_right);
        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnConfirmCrop = findViewById(R.id.btn_confirm_crop);

        btnRotateLeft.setOnClickListener(v -> rotateImage(-90)); // Rotate counter-clockwise
        btnRotateRight.setOnClickListener(v -> rotateImage(90));  // Rotate clockwise
        btnCancel.setOnClickListener(v -> cancelEdit());         // Cancel editing
        btnConfirmCrop.setOnClickListener(v -> confirmCrop());   // Confirm crop and return result

        // Get the image URI passed from the calling activity
        originalImageUri = getIntent().getData();
        if (originalImageUri == null) {
            Toast.makeText(this, "No image provided for editing.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no image URI is provided
            return;
        }

        // Load the image bitmap asynchronously to prevent UI freezing
        // This is important for large images.
        loadImageAsync(originalImageUri);
    }

    /**
     * Loads the image bitmap from the given URI in a background thread.
     * It decodes the bitmap efficiently by sampling it down to fit the ImageView's dimensions,
     * preventing OutOfMemoryErrors for large images.
     * Once loaded, it updates the ImageView and the CropOverlayView on the UI thread.
     * @param imageUri URI of the image to load.
     */
    private void loadImageAsync(Uri imageUri) {
        new Thread(() -> {
            try {
                // Get ImageView dimensions. If not yet measured, use a reasonable default.
                // onWindowFocusChanged might be better for precise initial dimensions,
                // but this often works well enough for initial sampling.
                int imageViewWidth = imageView.getWidth();
                int imageViewHeight = imageView.getHeight();
                if (imageViewWidth == 0 || imageViewHeight == 0) {
                    // Fallback if ImageView hasn't been laid out yet
                    imageViewWidth = getResources().getDisplayMetrics().widthPixels;
                    imageViewHeight = getResources().getDisplayMetrics().heightPixels;
                }

                // First, decode bounds to get original image dimensions without loading full bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                InputStream is = getContentResolver().openInputStream(imageUri);
                if (is != null) {
                    BitmapFactory.decodeStream(is, null, options);
                    is.close();
                }

                // Calculate inSampleSize (downsampling factor)
                options.inSampleSize = calculateInSampleSize(options, imageViewWidth, imageViewHeight);

                // Decode bitmap with the calculated inSampleSize
                options.inJustDecodeBounds = false; // Now decode the actual bitmap
                is = getContentResolver().openInputStream(imageUri);
                if (is != null) {
                    originalBitmap = BitmapFactory.decodeStream(is, null, options);
                    is.close();
                }

                if (originalBitmap == null) {
                    // Handle case where bitmap could not be decoded
                    runOnUiThread(() -> {
                        Toast.makeText(ImageEditActivity.this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // Update UI elements on the main thread
                runOnUiThread(() -> {
                    imageView.setImageBitmap(originalBitmap);
                    // Pass the loaded bitmap and the ImageView to the CropOverlayView
                    // The overlay needs the bitmap to calculate its natural size for cropping,
                    // and the ImageView to understand how the bitmap is displayed (scaled within ImageView).
                    cropOverlayView.setImage(originalBitmap, imageView);
                });

            } catch (IOException e) {
                // Log and show error if image loading fails
                Log.e(TAG, "Error loading image: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ImageEditActivity.this, "Error loading image.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    /**
     * Helper method to calculate an appropriate inSampleSize for downsampling a bitmap.
     * This prevents loading excessively large images into memory.
     * @param options BitmapFactory.Options containing original image dimensions (inJustDecodeBounds = true).
     * @param reqWidth The desired width for the downsampled bitmap.
     * @param reqHeight The desired height for the downsampled bitmap.
     * @return The inSampleSize value (power of 2) to use for decoding.
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than or equal to the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    /**
     * Rotates the current originalBitmap by the specified degrees (e.g., -90 for left, 90 for right).
     * Creates a new rotated bitmap, updates the ImageView, and recycles the old one.
     * @param degrees The degrees to rotate (e.g., -90 or 90).
     */
    private void rotateImage(int degrees) {
        if (originalBitmap == null) {
            // Cannot rotate if no bitmap is loaded
            return;
        }

        // Update total rotation degrees for display purposes (optional)
        currentRotationDegrees = (currentRotationDegrees + degrees) % 360;
        if (currentRotationDegrees < 0) {
            currentRotationDegrees += 360; // Ensure positive angle
        }

        // Create a transformation matrix for rotation
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees); // Apply rotation

        // Create a new bitmap from the original, applying the rotation
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0, // Start X
                0, // Start Y
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                matrix,
                true // Filter: true for smooth scaling/rotation
        );

        // Recycle the old bitmap to free up memory, only if it's a different instance
        if (originalBitmap != rotatedBitmap && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        originalBitmap = rotatedBitmap; // Update reference to the new rotated bitmap

        // Update ImageView and CropOverlayView with the newly rotated bitmap
        imageView.setImageBitmap(originalBitmap);
        cropOverlayView.setImage(originalBitmap, imageView);
        Toast.makeText(this, "Rotated to " + currentRotationDegrees + " degrees", Toast.LENGTH_SHORT).show();
    }


    /**
     * Crops the image based on the current crop rectangle defined by CropOverlayView.
     * It then saves the cropped bitmap to a temporary file and returns its URI to the calling activity.
     */
    private void confirmCrop() {
        if (originalBitmap == null) {
            cancelEdit(); // No image to crop, just cancel
            return;
        }

        // Get the crop rectangle coordinates relative to the original bitmap's pixels
        RectF cropRectInImage = cropOverlayView.getCropRectInImageCoordinates();

        if (cropRectInImage == null || cropRectInImage.isEmpty()) {
            // No valid crop area selected or calculated
            Toast.makeText(this, "No valid crop area selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract integer coordinates for Bitmap.createBitmap
        // Clamp values to ensure they are within the bitmap's bounds
        int x = (int) cropRectInImage.left;
        int y = (int) cropRectInImage.top;
        int width = (int) cropRectInImage.width();
        int height = (int) cropRectInImage.height();

        x = Math.max(0, x);
        y = Math.max(0, y);
        // Ensure width/height don't go beyond bitmap boundaries
        width = Math.min(width, originalBitmap.getWidth() - x);
        height = Math.min(height, originalBitmap.getHeight() - y);

        if (width <= 0 || height <= 0) {
            Toast.makeText(this, "Invalid crop dimensions. Please select a valid area.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap croppedBitmap = null;
        try {
            // Create the new cropped bitmap
            croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, width, height);
        } catch (IllegalArgumentException e) {
            // Catch potential errors if the crop rectangle is invalid (e.g., negative dimensions)
            Log.e(TAG, "Error creating cropped bitmap: " + e.getMessage());
            Toast.makeText(this, "Error cropping image.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the cropped bitmap to a temporary file in the app's cache directory
        File outputDir = new File(getCacheDir(), "images"); // 'images' is a subdirectory in cache
        if (!outputDir.exists()) {
            outputDir.mkdirs(); // Create directories if they don't exist
        }
        File outputFile = new File(outputDir, "cropped_image_" + System.currentTimeMillis() + ".jpg");

        Uri resultUri = null;
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            // Compress the bitmap to JPEG format with 90% quality
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            // Get a content URI for the saved file using FileProvider
            resultUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider", // IMPORTANT: This must match the authority in AndroidManifest.xml
                    outputFile
            );

            // Set the result for the calling activity and finish
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_RESULT_URI, resultUri);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();

        } catch (IOException e) {
            // Handle errors during file saving
            Log.e(TAG, "Error saving cropped image: " + e.getMessage());
            Toast.makeText(this, "Failed to save cropped image.", Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_CANCELED); // Indicate failure
            finish();
        } finally {
            // Recycle the croppedBitmap immediately after it's saved to free memory
            if (croppedBitmap != null && !croppedBitmap.isRecycled()) {
                croppedBitmap.recycle();
            }
            // Recycle the originalBitmap as it's no longer needed after processing
            if (originalBitmap != null && !originalBitmap.isRecycled()) {
                originalBitmap.recycle();
                originalBitmap = null; // Clear reference
            }
        }
    }

    /**
     * Cancels the image editing process and returns RESULT_CANCELED to the calling activity.
     */
    private void cancelEdit() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Crucially, recycle the bitmap when the activity is destroyed
        // to prevent memory leaks.
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
            originalBitmap = null;
        }
    }
}
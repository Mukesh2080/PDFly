package com.mukesh.pdfly.signature.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mukesh.pdfly.imageEditor.activity.ImageEditActivity;
import com.mukesh.pdfly.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImportSignatureFragment extends Fragment {

    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_CODE_CROP_IMAGE = 102;

    private ImageView importedImageView;
    private Bitmap selectedBitmap;
    private Uri sourceImageUri;

    private Button btnPickImage, btnUseSignature;

    private OnSignatureImportedListener callback;

    public interface OnSignatureImportedListener {
        void onSignatureImported(Bitmap transparentSignature);
    }

    public void setOnSignatureImportedListener(OnSignatureImportedListener listener) {
        this.callback = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import_signature, container, false);

        importedImageView = view.findViewById(R.id.imagePreview);
        btnPickImage = view.findViewById(R.id.btnSelectImage);
        btnUseSignature = view.findViewById(R.id.btnSave);

        btnPickImage.setOnClickListener(v -> openImagePicker());

        btnUseSignature.setOnClickListener(v -> {
            if (selectedBitmap != null) {
                Bitmap transparent = makeWhiteBackgroundTransparent(selectedBitmap);
                saveSignatureToFile(transparent);
            }
        });

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            sourceImageUri = data.getData();
            startImageEdit(sourceImageUri);
//            new AlertDialog.Builder(requireContext())
//                    .setTitle("Crop Image?")
//                    .setMessage("Would you like to crop the selected image before using it?")
//                    .setPositiveButton("Yes", (dialog, which) -> cropImage(sourceImageUri))
//                    .setNegativeButton("No", (dialog, which) -> {
//                        try {
//                            selectedBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), sourceImageUri);
//                            selectedBitmap = makeWhiteBackgroundTransparent(selectedBitmap);
//                            importedImageView.setImageBitmap(selectedBitmap);
//                        } catch (IOException e) {
//                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .show();

        } else if (requestCode == REQUEST_CODE_CROP_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri editedImageUri = data.getParcelableExtra(ImageEditActivity.EXTRA_RESULT_URI);
            if (editedImageUri != null) {
                try {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), editedImageUri);
                            selectedBitmap = makeWhiteBackgroundTransparent(selectedBitmap);
                            importedImageView.setImageBitmap(selectedBitmap);
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                Toast.makeText(getContext(), "Image edited successfully: " + editedImageUri.toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Edited image URI not found.", Toast.LENGTH_SHORT).show();
            }
//            Bundle extras = data.getExtras();
//            if (extras != null) {
//                Bitmap cropped = extras.getParcelable("data");
//                if (cropped != null) {
//                    selectedBitmap = makeWhiteBackgroundTransparent(cropped);
//                    importedImageView.setImageBitmap(selectedBitmap);
//                }
//            }
        }
    }

    private void cropImage(Uri uri) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(uri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 512);
            cropIntent.putExtra("outputY", 512);
            cropIntent.putExtra("return-data", true);
            startActivityForResult(cropIntent, REQUEST_CODE_CROP_IMAGE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Crop not supported on this device", Toast.LENGTH_SHORT).show();
            // fallback: load directly
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                selectedBitmap = makeWhiteBackgroundTransparent(selectedBitmap);
                importedImageView.setImageBitmap(selectedBitmap);
            } catch (IOException ioException) {
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap makeWhiteBackgroundTransparent(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int backgroundColor = original.getPixel(0, 0); // top-left pixel
        int tolerance = 30; // increase for more aggressive removal

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = original.getPixel(x, y);
                if (isSimilarColor(pixel, backgroundColor, tolerance)) {
                    result.setPixel(x, y, Color.TRANSPARENT);
                } else {
                    result.setPixel(x, y, pixel);
                }
            }
        }

        return result;
    }

    // Helper function to compare colors with tolerance
    private boolean isSimilarColor(int pixelColor, int bgColor, int tolerance) {
        int r1 = Color.red(pixelColor);
        int g1 = Color.green(pixelColor);
        int b1 = Color.blue(pixelColor);

        int r2 = Color.red(bgColor);
        int g2 = Color.green(bgColor);
        int b2 = Color.blue(bgColor);

        return Math.abs(r1 - r2) < tolerance &&
                Math.abs(g1 - g2) < tolerance &&
                Math.abs(b1 - b2) < tolerance;
    }

    private void saveSignatureToFile(Bitmap bitmap) {
        File dir = new File(requireContext().getFilesDir(), "signatures");
        if (!dir.exists()) dir.mkdirs();

        String filename = "sig_" + System.currentTimeMillis() + ".png";
        File file = new File(dir, filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Toast.makeText(getContext(), "Signature saved", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving", Toast.LENGTH_SHORT).show();
        }
    }

    // Example: When a button is clicked or an image is selected from gallery
    public void startImageEdit(Uri imageToEditUri) {
        if (imageToEditUri == null) {
            Toast.makeText(getContext(), "No image to edit selected.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent editIntent = new Intent(getContext(), ImageEditActivity.class);
        editIntent.setData(imageToEditUri); // Pass the Uri as the Intent's data
        startActivityForResult(editIntent, REQUEST_CODE_CROP_IMAGE); // Launch for result
    }
}

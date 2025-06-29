package com.mukesh.pdfly.signature.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mukesh.pdfly.R;
import com.mukesh.pdfly.signature.views.SignatureDrawView;
import com.mukesh.pdfly.signature.activity.SignatureCreatorActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DrawSignatureFragment extends Fragment {

    private SignatureDrawView drawView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_draw_signature, container, false);

        drawView = view.findViewById(R.id.signatureDrawView);
        drawView.setDrawListener(new SignatureDrawView.SignatureDrawListener() {
            @Override
            public void onStartDrawing() {
                if (getActivity() instanceof SignatureCreatorActivity) {
                    ((SignatureCreatorActivity) getActivity()).setSwipeEnabled(false);
                }
            }

            @Override
            public void onStopDrawing() {
                if (getActivity() instanceof SignatureCreatorActivity) {
                    ((SignatureCreatorActivity) getActivity()).setSwipeEnabled(true);
                }
            }
        });

        Button saveButton = view.findViewById(R.id.btnSaveSignature);
        Button clearBtn = view.findViewById(R.id.clear_sign);

        saveButton.setOnClickListener(v -> {
            Bitmap signature = drawView.exportAsBitmap();
            saveSignatureToFile(signature);
        });
        clearBtn.setOnClickListener(v -> {
           drawView.clear();
        });

        return view;
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
}


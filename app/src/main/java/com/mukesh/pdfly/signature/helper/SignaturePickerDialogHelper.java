package com.mukesh.pdfly.signature.helper;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
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
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mukesh.pdfly.R;
import com.mukesh.pdfly.signature.adapter.SignatureAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SignaturePickerDialogHelper {

    public interface SignatureSelectionListener {
        void onSignatureSelected(Bitmap signature);
        void onAddNewSignatureRequested();
    }

    private final Activity activity;
    private final SignatureSelectionListener listener;
    private SignatureAdapter adapter;

    public SignaturePickerDialogHelper(Activity activity, SignatureSelectionListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void show() {
        Dialog dialog = new Dialog(activity);
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_signature_picker, null);

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

        RecyclerView recyclerView = sheetView.findViewById(R.id.signatureRecyclerView);
        ImageButton btnAdd = sheetView.findViewById(R.id.btnAddSignature);

        List<File> signatures = loadSavedSignatureFiles();
        adapter = new SignatureAdapter(signatures, selected -> {
            listener.onSignatureSelected(selected);
            dialog.dismiss();
        }, (position, file) -> {
            if (file.exists()) file.delete();
            adapter.removeAt(position);
            Toast.makeText(activity, "Signature deleted", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onAddNewSignatureRequested();
        });

        dialog.show();
    }

    private List<File> loadSavedSignatureFiles() {
        List<File> result = new ArrayList<>();
        File dir = new File(activity.getFilesDir(), "signatures");
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                result.add(file);
            }
        }
        return result;
    }
}


package com.mukesh.pdfly;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_PDF = 1001;
    private Button selectPdfButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectPdfButton = findViewById(R.id.selectPdfButton);
        selectPdfButton.setOnClickListener(v -> openPdfPicker());
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OPEN_PDF && resultCode == RESULT_OK && data != null) {
            Uri pdfUri = data.getData();

            // Take persistable URI permission so we can reopen the file later
            getContentResolver().takePersistableUriPermission(
                    pdfUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            // Open the editor
            Intent intent = new Intent(this, PdfEditorActivity.class);
            intent.putExtra("pdfUri", pdfUri.toString());
            intent.putExtra("isEditable", true); // or false for read-only mode
            startActivity(intent);
        }
    }
}

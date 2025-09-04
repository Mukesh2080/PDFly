package com.mukesh.pdfly;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.mukesh.pdfly.helper.CustomTypefaceSpan;
import com.mukesh.pdfly.pdfrenderer.activity.PdfEditorActivity;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_CODE_OPEN_PDF = 1001;
    private Button selectPdfButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyEdgeToEdgeInsets(R.id.appbar,R.id.rootLayout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Load custom font from res/font
        Typeface typeface = ResourcesCompat.getFont(this, R.font.huricanreg);

// Replace default title with custom-styled Spannable
        SpannableString s = new SpannableString("PDFly");
        s.setSpan(new CustomTypefaceSpan(typeface), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getSupportActionBar().setTitle(s);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

        }
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
            intent.setData(pdfUri);
//            intent.putExtra("pdfUri", pdfUri.toString());
//            intent.putExtra("isEditable", true); // or false for read-only mode
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

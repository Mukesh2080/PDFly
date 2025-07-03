package com.mukesh.pdfly;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.rendering.PDFRenderer;


public class PdfBoxEditorActivity2 extends AppCompatActivity {

    private Uri pdfUri;
    private PDDocument document;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_box_editor);
        PDFBoxResourceLoader.init(getApplicationContext());

        pdfUri = getIntent().getData();
        if (pdfUri == null) {
            Toast.makeText(this, "Invalid PDF URI", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            openPdfFromUri(pdfUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openPdfFromUri(Uri uri) throws IOException {
        Button addtext = findViewById(R.id.btnAddText);
        addtext.setOnClickListener(view -> {
            addTextToPage("mukesh",0,0,0);
        });
        LinearLayout pdfPageContainer = findViewById(R.id.pdfPageContainer);

        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
        PdfRenderer renderer = new PdfRenderer(pfd);

        for (int i = 0; i < renderer.getPageCount(); i++) {
            PdfRenderer.Page page = renderer.openPage(i);
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(0, 8, 0, 8);
            pdfPageContainer.addView(imageView);
        }
        renderer.close();


    }

    private void addTextToPage(String text, int pageIndex, float x, float y) {
        try {
            PDPage page = document.getPage(pageIndex);
            PDPageContentStream stream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
            stream.beginText();
            stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePdf() {
        try {
            File outFile = new File(getExternalFilesDir(null), "edited_output.pdf");
            document.save(outFile);
            Toast.makeText(this, "Saved to: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (document != null) {
                document.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.mukesh.pdfly.pdfrenderer.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//mukesh
public class PdfRendererHelper {

    public interface OnPageRenderedListener {
        void onPageRendered(Bitmap renderedPage, int pageIndex);
    }

    public static void renderAllPages(Context context, Uri pdfUri, OnPageRenderedListener listener) {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(pdfUri, "r")) {
            if (pfd != null) {
                renderFromParcel(context, pfd, listener);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to render PDF", Toast.LENGTH_SHORT).show();
        }
    }

    public static void renderAllPages(Context context, String filePath, OnPageRenderedListener listener) {
        File file = new File(filePath);
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
            renderFromParcel(context, pfd, listener);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to render PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private static void renderFromParcel(Context context, ParcelFileDescriptor pfd, OnPageRenderedListener listener) throws IOException {
        PdfRenderer renderer = new PdfRenderer(pfd);
        int pageCount = renderer.getPageCount();
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
// Get screen size for safe rendering dimensions
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int maxWidth = metrics.widthPixels;
        int maxHeight = metrics.heightPixels;
        for (int i = 0; i < pageCount; i++) {
            PdfRenderer.Page page = renderer.openPage(i);
            int pageWidth = page.getWidth();
            int pageHeight = page.getHeight();

            // Calculate scale to fit within screen size
            float scale = Math.min((float) maxWidth / pageWidth, (float) maxHeight / pageHeight);

            int renderWidth = (int) (pageWidth * scale);
            int renderHeight = (int) (pageHeight * scale);

            // Estimate bitmap size and skip if too large (optional safety)
            long estimatedBytes = renderWidth * renderHeight * 4L;
            if (estimatedBytes > 100 * 1024 * 1024) { // >100MB
                Log.e("PDFRenderer", "Bitmap too large, skipping page " + i);
                continue;
            }

            Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            listener.onPageRendered(bitmap, i);
        }

        renderer.close();
    }

}


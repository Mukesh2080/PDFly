package com.mukesh.pdfly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mukesh.pdfly.pdfrenderer.views.DrawView;

import java.util.ArrayList;
import java.util.List;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

    private final Context context;
    private final PdfRenderer renderer;
    private final boolean isDrawMode;
    private final List<DrawView> drawViews = new ArrayList<>();

    public PdfPageAdapter(Context ctx, PdfRenderer renderer, boolean isDrawMode) {
        this.context = ctx;
        this.renderer = renderer;
        this.isDrawMode = false;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bindPage(position);
    }

    @Override
    public int getItemCount() {
        return renderer.getPageCount();
    }

    class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView pdfImageView;
        DrawView drawView;

        PageViewHolder(View itemView) {
            super(itemView);
            pdfImageView = itemView.findViewById(R.id.pdfImageView);
            drawView = itemView.findViewById(R.id.drawView);
            drawView.setDrawingEnabled(isDrawMode);
        }

        void bindPage(int index) {
            // Render page bitmap scaled to screen width
            PdfRenderer.Page page = renderer.openPage(index);
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            float scale = (float) screenWidth / page.getWidth();
            int height = (int) (page.getHeight() * scale);

            Bitmap bitmap = Bitmap.createBitmap(screenWidth, height, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            pdfImageView.setImageBitmap(bitmap);
            drawView.setDrawingEnabled(true);
            if (!drawViews.contains(drawView)) drawViews.add(drawView);
        }
    }
}


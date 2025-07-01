package com.mukesh.pdfly.pdfrenderer.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mukesh.pdfly.R;

import java.io.File;
import java.util.List;

public class PdfItemAdapter extends RecyclerView.Adapter<PdfItemAdapter.PdfViewHolder> {

    private List<File> pdfFiles;
    private Context context;

    public PdfItemAdapter(Context context, List<File> pdfFiles) {
        this.context = context;
        this.pdfFiles = pdfFiles;
    }

    @Override
    public PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.pdf_item_layout, parent, false);
        return new PdfViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PdfViewHolder holder, int position) {
        File file = pdfFiles.get(position);
        holder.name.setText(file.getName());
        holder.size.setText(Formatter.formatFileSize(context, file.length()));

        holder.itemView.setOnClickListener(v -> {
            // Open the selected PDF in editor
//            Intent intent = new Intent(context, PdfEditorActivity.class);
//            intent.putExtra("pdfUri", Uri.fromFile(file));
//            intent.putExtra("isEditable", true); // Editable mode
//            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    public void updateList(List<File> newList) {
        this.pdfFiles = newList;
        notifyDataSetChanged();
    }


    public class PdfViewHolder extends RecyclerView.ViewHolder {

        TextView name, size;

        public PdfViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.pdfName);
            size = itemView.findViewById(R.id.pdfSize);
        }
    }
}


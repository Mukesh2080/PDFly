package com.mukesh.pdfly.signature.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mukesh.pdfly.R;

import java.io.File;
import java.util.List;

public class SignatureAdapter extends RecyclerView.Adapter<SignatureAdapter.SignatureViewHolder> {

    private int selectedPosition = -1; // Track selected position
    private final List<File> signatures;
    private final OnSignatureClickListener listener;
    private final OnSignatureDeleteListener deleteListener;


    public interface OnSignatureClickListener {
        void onSignatureSelected(Bitmap signature);
    }
    public interface OnSignatureDeleteListener {
        void onSignatureDeleted(int position, File file);
    }

    public SignatureAdapter(List<File> signatures, OnSignatureClickListener listener, OnSignatureDeleteListener deleteListener) {
        this.signatures = signatures;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public SignatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_signature_preview, parent, false);
        return new SignatureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SignatureViewHolder holder, int position) {
        File signatureFile = signatures.get(position);
        Bitmap signature = BitmapFactory.decodeFile(signatureFile.getAbsolutePath());
        holder.imageSignature.setImageBitmap(signature);

        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.selected_signature_bg);
        } else {
            holder.itemView.setBackgroundResource(0); // Remove background
        }
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            // Notify new item to show selection
            notifyItemChanged(selectedPosition);
            listener.onSignatureSelected(signature);
            holder.mView.setBackgroundResource(R.drawable.selected_signature_bg);
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onSignatureDeleted(position, signatureFile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return signatures.size();
    }

    public void removeAt(int position) {
        signatures.remove(position);
        notifyItemRemoved(position);
    }
    static class SignatureViewHolder extends RecyclerView.ViewHolder {
        View mView;
        ImageView imageSignature;
        ImageButton deleteButton;


        SignatureViewHolder(View itemView) {
            super(itemView);
            mView = itemView.getRootView();
            imageSignature = itemView.findViewById(R.id.imageSignature);
            deleteButton = itemView.findViewById(R.id.deleteSignature);
        }
    }
}


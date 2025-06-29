package com.mukesh.pdfly.signature.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mukesh.pdfly.R;

import java.util.List;

public class SignatureAdapter extends RecyclerView.Adapter<SignatureAdapter.SignatureViewHolder> {
    private final List<Bitmap> signatures;
    private final OnSignatureClickListener listener;

    public interface OnSignatureClickListener {
        void onSignatureSelected(Bitmap signature);
    }

    public SignatureAdapter(List<Bitmap> signatures, OnSignatureClickListener listener) {
        this.signatures = signatures;
        this.listener = listener;
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
        Bitmap signature = signatures.get(position);
        holder.imageSignature.setImageBitmap(signature);
        holder.itemView.setOnClickListener(v -> listener.onSignatureSelected(signature));
    }

    @Override
    public int getItemCount() {
        return signatures.size();
    }

    static class SignatureViewHolder extends RecyclerView.ViewHolder {
        ImageView imageSignature;

        SignatureViewHolder(View itemView) {
            super(itemView);
            imageSignature = itemView.findViewById(R.id.imageSignature);
        }
    }
}


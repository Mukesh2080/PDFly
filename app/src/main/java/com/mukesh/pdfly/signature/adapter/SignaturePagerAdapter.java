package com.mukesh.pdfly.signature.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.mukesh.pdfly.signature.fragment.DrawSignatureFragment;
import com.mukesh.pdfly.signature.fragment.ImportSignatureFragment;

public class SignaturePagerAdapter extends FragmentStateAdapter {

    public SignaturePagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return (position == 0) ? new DrawSignatureFragment() : new ImportSignatureFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}


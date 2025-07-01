package com.mukesh.pdfly.signature.activity;

import android.os.Bundle;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.mukesh.pdfly.R;
import com.mukesh.pdfly.signature.adapter.SignaturePagerAdapter;
import com.mukesh.pdfly.signature.helper.ViewPager2SwipeDisabler;

public class SignatureCreatorActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private SignaturePagerAdapter adapter;
    ViewPager2SwipeDisabler swipeDisabler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature_creator);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        swipeDisabler = new ViewPager2SwipeDisabler(viewPager);

        adapter = new SignaturePagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // Set both text and icon for each tab
                    if (position == 0) {
                        tab.setText("Draw");
                        tab.setIcon(R.drawable.ic_draw_sign);
                    } else {
                        tab.setText("Import");
                        tab.setIcon(R.drawable.ic_file_signature_24dp);
                    }

                    // Optional: Customize tab appearance
                    tab.view.setGravity(Gravity.CENTER);
                }
        ).attach();
    }
    public void setSwipeEnabled(boolean enabled) {
        if (swipeDisabler != null) {
            swipeDisabler.setSwipeEnabled(enabled);
        }
    }
}

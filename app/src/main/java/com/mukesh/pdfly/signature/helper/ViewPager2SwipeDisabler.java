package com.mukesh.pdfly.signature.helper;

import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class ViewPager2SwipeDisabler {
    private final ViewPager2 viewPager2;
    private boolean swipeEnabled = true;

    public ViewPager2SwipeDisabler(ViewPager2 viewPager2) {
        this.viewPager2 = viewPager2;
    }

    public void setSwipeEnabled(boolean enabled) {
        swipeEnabled = enabled;
        if (viewPager2 != null) {
            // Method 1: Disable user input (works for simple cases)
            viewPager2.setUserInputEnabled(enabled);

            // Method 2: More comprehensive approach
            RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
            if (recyclerView != null) {
                recyclerView.setOnTouchListener(enabled ? null : (v, event) -> {
                    // For horizontal swipe gestures, still allow vertical scrolling if needed
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (Math.abs(event.getX() - event.getHistoricalX(0)) >
                                Math.abs(event.getY() - event.getHistoricalY(0))) {
                            return true; // Block horizontal swipes
                        }
                    }
                    return false; // Allow vertical scrolling
                });
            }
        }
    }

    public boolean isSwipeEnabled() {
        return swipeEnabled;
    }
}
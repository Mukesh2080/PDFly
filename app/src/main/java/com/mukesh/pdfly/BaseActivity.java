package com.mukesh.pdfly;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);
    }

    protected void applyEdgeToEdgeInsets(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            View target = findViewById(viewId);
            if (target != null) {
                ViewCompat.setOnApplyWindowInsetsListener(target, (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                    );

                    boolean applyTop = v.getId() == R.id.appbar ;
                    boolean applyBottom =  v.getId() == R.id.rootLayout;

                    v.setPadding(
                            v.getPaddingLeft(),
                            applyTop ? systemBars.top : v.getPaddingTop(),
                            v.getPaddingRight(),
                            applyBottom ? systemBars.bottom : v.getPaddingBottom()
                    );

                    return insets;
                });
            }
        }
    }


}


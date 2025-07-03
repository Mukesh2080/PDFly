package com.mukesh.pdfly;

import android.app.Application;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(getApplicationContext());
    }
}

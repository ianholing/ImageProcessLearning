package com.metodica.imageprocess;

import android.app.Application;

/**
 * Created by Jacob on 11/25/13.
 */
public class MainApp extends Application {

    private static MainApp singleton;

    public static MainApp getInstance() {
        return singleton;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        singleton = this;
    }
}
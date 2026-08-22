package com.wifikeepalive;

import android.app.Application;
import android.util.Log;

public class KeepAliveApp extends Application {
    private static final String TAG = "WifiKeepAlive";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "应用启动");
    }
}

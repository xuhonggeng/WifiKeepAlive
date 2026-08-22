package com.wifikeepalive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "WifiKeepAlive";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity启动");

        startKeepAliveService();
        requestIgnoreBatteryOptimizations();

        // 隐藏图标（首次运行后）
        // hideLauncherIcon();

        finish();
    }

    private void startKeepAliveService() {
        Intent serviceIntent = new Intent(this, WifiKeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "无法请求电池优化忽略");
            }
        }
    }

    /*
    private void hideLauncherIcon() {
        getPackageManager().setComponentEnabledSetting(
            new android.content.ComponentName(this, MainActivity.class),
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            android.content.pm.PackageManager.DONT_KILL_APP
        );
    }
    */
}

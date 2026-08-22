package com.wifikeepalive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

public class WifiKeepAliveService extends Service {
    private static final String TAG = "WifiKeepAlive";
    private static final String CHANNEL_ID = "wifi_keep_alive";
    private static final int NOTIFICATION_ID = 1001;

    private ConnectivityManager mCm;
    private ConnectivityManager.NetworkCallback mCallback;
    private Network mWifiNetwork;
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler;
    private Runnable mCheckRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WifiKeepAlive::Lock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        startKeepAlive();
        startMonitor();

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire(10 * 60 * 1000L);
        }

        return START_STICKY;
    }

    private void startKeepAlive() {
        if (mCallback != null) return;

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

            mCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    mWifiNetwork = network;
                    Log.i(TAG, "WiFi已绑定: " + network);
                }

                @Override
                public void onLost(Network network) {
                    if (network.equals(mWifiNetwork)) {
                        mWifiNetwork = null;
                    }
                    Log.w(TAG, "WiFi丢失");
                }
            };

            mCm.requestNetwork(request, mCallback);
            Log.i(TAG, "保活已启动");

        } catch (Exception e) {
            Log.e(TAG, "启动失败: " + e.getMessage());
        }
    }

    private void startMonitor() {
        mCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (mCallback == null || mWifiNetwork == null) {
                    Log.w(TAG, "保活异常，重新启动");
                    stopKeepAlive();
                    startKeepAlive();
                }
                mHandler.postDelayed(this, 30000);
            }
        };
        mHandler.postDelayed(mCheckRunnable, 30000);
    }

    private void stopKeepAlive() {
        try {
            if (mCallback != null) {
                mCm.unregisterNetworkCallback(mCallback);
            }
        } catch (Exception e) {}
        mCallback = null;
        mWifiNetwork = null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "WiFi保活", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setContentTitle("WiFi保活服务")
            .setContentText("保持WiFi与有线同时在线")
            .setSmallIcon(android.R.drawable.ic_menu_network)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopKeepAlive();
        mHandler.removeCallbacks(mCheckRunnable);
        if (mWakeLock.isHeld()) mWakeLock.release();

        // 尝试重启
        Intent restart = new Intent(this, WifiKeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restart);
        } else {
            startService(restart);
        }
    }
}

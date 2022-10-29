package com.syncloudsoft.taktak;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Collections;

public class MainApplication extends Application {

    private static HttpProxyCacheServer mProxyServer;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
        if (BuildConfig.DEBUG) {
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(
                            Collections.singletonList(getString(R.string.admob_test_device_id)))
                    .build();
            MobileAds.setRequestConfiguration(configuration);
        }

        AppEventsLogger.activateApp(this);
        MobileAds.initialize(this, status -> { /* eaten */ });
    }

    public synchronized static HttpProxyCacheServer getProxyServer(Context context) {
        if (mProxyServer == null) {
            mProxyServer = new HttpProxyCacheServer.Builder(context.getApplicationContext())
                    .maxCacheSize(256 * 1024 * 1024)
                    .maxCacheFilesCount(100)
                    .build();
        }

        return mProxyServer;
    }
}

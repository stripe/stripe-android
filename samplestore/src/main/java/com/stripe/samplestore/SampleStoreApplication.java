package com.stripe.samplestore;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

public class SampleStoreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG && LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        if (BuildConfig.DEBUG) {
            LeakCanary.install(this);
        }
    }
}

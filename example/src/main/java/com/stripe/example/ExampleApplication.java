package com.stripe.example;

import android.support.multidex.MultiDexApplication;

import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

public class ExampleApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);

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

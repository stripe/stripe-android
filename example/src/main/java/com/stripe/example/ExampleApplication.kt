package com.stripe.example

import android.os.StrictMode
import android.support.multidex.MultiDexApplication

import com.facebook.stetho.Stetho
import com.squareup.leakcanary.LeakCanary

class ExampleApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .penaltyDeath()
            .build())

        Stetho.initializeWithDefaults(this)

        if (BuildConfig.DEBUG && LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        if (BuildConfig.DEBUG) {
            LeakCanary.install(this)
        }
    }
}

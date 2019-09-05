package com.stripe.example

import android.os.StrictMode
import androidx.multidex.MultiDexApplication

import com.facebook.stetho.Stetho
import com.squareup.leakcanary.LeakCanary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExampleApplication : MultiDexApplication() {

    override fun onCreate() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build())

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build())

        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            Stetho.initializeWithDefaults(this@ExampleApplication)
        }

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

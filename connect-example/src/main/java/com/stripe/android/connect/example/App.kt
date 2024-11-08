package com.stripe.android.connect.example

import android.app.Application
import android.os.StrictMode
import com.stripe.android.connect.example.data.EmbeddedComponentManagerWrapper
import com.stripe.android.connect.example.data.SettingsService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )

        SettingsService.init(this)
        EmbeddedComponentManagerWrapper.init()
    }
}

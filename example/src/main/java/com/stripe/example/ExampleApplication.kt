package com.stripe.example

import android.os.StrictMode
import androidx.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.example.service.ExampleEphemeralKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExampleApplication : MultiDexApplication() {

    override fun onCreate() {
        PaymentConfiguration.init(this, Settings(this).publishableKey)

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

        CustomerSession.initCustomerSession(
            this,
            ExampleEphemeralKeyProvider(this),
            false
        )
    }
}

package com.stripe.example

import android.app.Application
import android.os.StrictMode
import com.facebook.stetho.Stetho
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.example.service.ExampleEphemeralKeyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExampleApplication : Application() {

    override fun onCreate() {
        PaymentConfiguration.init(this, Settings(this).publishableKey)

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .also {
                    if (IS_PENALTY_DEATH_ENABLED) {
                        it.penaltyDeath()
                    }
                }
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )

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

    private companion object {
        private val IS_PENALTY_DEATH_ENABLED = false
    }
}

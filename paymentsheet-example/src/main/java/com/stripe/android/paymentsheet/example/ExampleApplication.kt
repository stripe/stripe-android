package com.stripe.android.paymentsheet.example

import android.app.Application
import android.os.StrictMode

class ExampleApplication : Application() {

    override fun onCreate() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
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
                .detectAll()
                .penaltyLog()
                .also {
                    if (IS_PENALTY_DEATH_ENABLED) {
                        it.penaltyDeath()
                    }
                }
                .build()
        )

        super.onCreate()
    }

    private companion object {
        private const val IS_PENALTY_DEATH_ENABLED = false
    }
}

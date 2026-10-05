package com.stripe.android.upidemo

import android.app.Application
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ConnectionFactory

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DemoStore.initialize(this)
        // This dedicated app never sends SDK requests to Stripe. Every response is local fake data.
        ConnectionFactory.Default.connectionOpener = DemoConnectionOpener()
        AnalyticsRequestExecutor.ENABLED = false
        PaymentConfiguration.init(this, "pk_test_upi_demo_offline")
    }
}

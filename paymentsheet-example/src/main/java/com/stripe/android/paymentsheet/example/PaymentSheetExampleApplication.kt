package com.stripe.android.paymentsheet.example

import android.app.Application
import com.stripe.android.PaymentConfiguration

class PaymentSheetExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(this, Settings(this).publishableKey)
    }
}

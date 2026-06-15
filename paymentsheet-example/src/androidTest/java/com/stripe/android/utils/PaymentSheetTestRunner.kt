package com.stripe.android.utils

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

internal class PaymentSheetTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        configureLeakCanaryForManagedDevices()
    }
}

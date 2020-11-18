package com.stripe.android.paymentsheet.ui

import androidx.appcompat.app.AppCompatActivity

internal abstract class BasePaymentSheetActivity : AppCompatActivity() {
    abstract fun onUserCancel()

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            onUserCancel()
        }
    }
}

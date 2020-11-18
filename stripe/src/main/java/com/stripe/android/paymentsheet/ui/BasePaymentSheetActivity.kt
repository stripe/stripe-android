package com.stripe.android.paymentsheet.ui

import androidx.appcompat.app.AppCompatActivity

internal abstract class BasePaymentSheetActivity<ResultType> : AppCompatActivity() {
    abstract fun onUserCancel()
    abstract fun hideSheet()
    abstract fun setActivityResult(result: ResultType)

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

    protected fun animateOut(
        result: ResultType
    ) {
        setActivityResult(result)
        hideSheet()
    }

    protected companion object {
        const val EXTRA_STARTER_ARGS = "com.stripe.android.paymentsheet.extra_starter_args"
    }
}

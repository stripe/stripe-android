package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentSheetActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<PaymentSheetActivity, PaymentSheetActivityStarter.Args>(
    activity,
    PaymentSheetActivity::class.java,
    REQUEST_CODE
) {
    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : ActivityStarter.Args {
        val googlePayConfig: PaymentSheet.GooglePayConfiguration? get() = config?.googlePay
        val isGooglePayEnabled: Boolean get() = googlePayConfig != null

        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(ActivityStarter.Args.EXTRA)
            }
        }
    }

    internal companion object {
        const val REQUEST_CODE: Int = 6003
    }
}

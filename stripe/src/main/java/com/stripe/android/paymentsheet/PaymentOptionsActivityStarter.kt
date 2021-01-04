package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentOptionsActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<PaymentOptionsActivity, PaymentOptionsActivityStarter.Args>(
    activity,
    PaymentOptionsActivity::class.java,
    REQUEST_CODE
) {
    @Parcelize
    data class Args(
        val paymentIntent: PaymentIntent,
        val paymentMethods: List<PaymentMethod>,
        val sessionId: SessionId,
        val config: PaymentSheet.Configuration?
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(ActivityStarter.Args.EXTRA)
            }
        }
    }

    internal companion object {
        const val REQUEST_CODE: Int = 6004
    }
}

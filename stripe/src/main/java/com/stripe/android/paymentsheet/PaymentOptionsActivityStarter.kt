package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentOptionsActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<PaymentOptionsActivity, PaymentOptionsActivityStarter.Args>(
    activity,
    PaymentOptionsActivity::class.java,
    REQUEST_CODE
) {
    sealed class Args : ActivityStarter.Args {
        abstract val paymentIntent: PaymentIntent
        abstract val paymentMethods: List<PaymentMethod>
        abstract val googlePayConfig: PaymentSheetGooglePayConfig?

        val isGooglePayEnabled: Boolean get() = googlePayConfig != null

        @Parcelize
        data class Default(
            override val paymentIntent: PaymentIntent,
            override val paymentMethods: List<PaymentMethod>,
            val customerConfiguration: PaymentSheet.CustomerConfiguration,
            override val googlePayConfig: PaymentSheetGooglePayConfig?
        ) : Args()

        @Parcelize
        data class Guest(
            override val paymentIntent: PaymentIntent,
            override val googlePayConfig: PaymentSheetGooglePayConfig?
        ) : Args() {
            override val paymentMethods: List<PaymentMethod>
                get() = emptyList()
        }

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

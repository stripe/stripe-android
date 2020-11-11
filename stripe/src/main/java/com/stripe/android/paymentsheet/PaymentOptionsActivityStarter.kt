package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.ActivityStarter
import kotlinx.android.parcel.Parcelize

internal class PaymentOptionsActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<PaymentOptionsActivity, PaymentOptionsActivityStarter.Args>(
    activity,
    PaymentOptionsActivity::class.java,
    REQUEST_CODE
) {
    sealed class Args : ActivityStarter.Args {
        @Parcelize
        data class Default(
            val paymentMethods: List<PaymentMethod>,
            val ephemeralKey: String,
            val customerId: String
        ) : Args()

        @Parcelize
        object Guest : Args()

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

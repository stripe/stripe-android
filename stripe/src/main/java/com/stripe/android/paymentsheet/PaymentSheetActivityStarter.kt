package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import com.stripe.android.view.ActivityStarter
import kotlinx.android.parcel.Parcelize

internal class PaymentSheetActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<PaymentSheetActivity, PaymentSheetActivityStarter.Args>(
    activity,
    PaymentSheetActivity::class.java,
    REQUEST_CODE
) {
    sealed class Args : ActivityStarter.Args {
        abstract val clientSecret: String

        @Parcelize
        data class Default(
            override val clientSecret: String,
            val ephemeralKey: String,
            val customerId: String
        ) : Args()

        @Parcelize
        data class Guest(
            override val clientSecret: String
        ) : Args()

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

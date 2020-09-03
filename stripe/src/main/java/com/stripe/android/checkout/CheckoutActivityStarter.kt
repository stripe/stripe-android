package com.stripe.android.checkout

import android.app.Activity
import android.content.Intent
import com.stripe.android.view.ActivityStarter
import kotlinx.android.parcel.Parcelize

internal class CheckoutActivityStarter : ActivityStarter<CheckoutActivity, CheckoutActivityStarter.Args> {

    internal constructor(activity: Activity) : super(
        activity,
        CheckoutActivity::class.java,
        REQUEST_CODE
    )

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val ephemeralKey: String,
        val customerId: String
    ) : ActivityStarter.Args {

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

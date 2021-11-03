package com.stripe.android.paymentsheet

import android.app.Activity
import com.stripe.android.view.ActivityStarter

internal class PaymentOptionsActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<PaymentOptionsActivity, PaymentOptionContract.Args>(
    activity,
    PaymentOptionsActivity::class.java,
    REQUEST_CODE
) {

    internal companion object {
        const val REQUEST_CODE: Int = 6004
    }
}

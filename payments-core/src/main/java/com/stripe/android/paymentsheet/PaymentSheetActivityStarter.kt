package com.stripe.android.paymentsheet

import android.app.Activity
import com.stripe.android.view.ActivityStarter

internal class PaymentSheetActivityStarter internal constructor(
    activity: Activity
) : ActivityStarter<PaymentSheetActivity, PaymentSheetContract.Args>(
    activity,
    PaymentSheetActivity::class.java,
    REQUEST_CODE
) {
    internal companion object {
        const val REQUEST_CODE: Int = 6003
    }
}

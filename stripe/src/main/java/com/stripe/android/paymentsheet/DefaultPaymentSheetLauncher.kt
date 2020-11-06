package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContract.Args>
) : PaymentSheetLauncher {
    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onComplete(it)
        }
    )

    override fun present(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration
    ) = present(
        PaymentSheetContract.Args(
            paymentIntentClientSecret,
            configuration
        )
    )

    override fun present(
        paymentIntentClientSecret: String
    ) = present(
        PaymentSheetContract.Args(
            paymentIntentClientSecret,
            config = null
        )
    )

    private fun present(args: PaymentSheetContract.Args) {
        activityResultLauncher.launch(args)
    }
}

package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.paymentsheet.analytics.SessionId

internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContract.Args>
) : PaymentSheetLauncher {
    private val sessionId: SessionId = SessionId()

    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onPaymentResult(it)
        }
    )

    override fun present(
        paymentIntentClientSecret: String,

        configuration: PaymentSheet.Configuration
    ) = present(
        PaymentSheetContract.Args(
            paymentIntentClientSecret,
            sessionId,
            configuration
        )
    )

    override fun present(
        paymentIntentClientSecret: String
    ) = present(
        PaymentSheetContract.Args(
            paymentIntentClientSecret,
            sessionId,
            config = null
        )
    )

    private fun present(args: PaymentSheetContract.Args) {
        activityResultLauncher.launch(args)
    }
}

package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.paymentsheet.analytics.SessionId

internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContract.Args>,
    private val statusBarColor: () -> Int?
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
        },

        // lazily access the statusBarColor in case the value changes between when this
        // class is instantiated and the payment sheet is launched
        { activity.window.statusBarColor }
    )

    override fun present(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration
    ) = present(
        PaymentSheetContract.Args(
            paymentIntentClientSecret,
            sessionId,
            statusBarColor(),
            configuration
        )
    )

    override fun present(
        paymentIntentClientSecret: String
    ) = present(
        PaymentSheetContract.Args(
            paymentIntentClientSecret,
            sessionId,
            statusBarColor(),
            config = null
        )
    )

    private fun present(args: PaymentSheetContract.Args) {
        activityResultLauncher.launch(args)
    }
}

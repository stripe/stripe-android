package com.stripe.android.paymentsheet

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.analytics.SessionId
import org.jetbrains.annotations.TestOnly

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
        { getStatusBarColor(activity) }
    )

    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onPaymentResult(it)
        },

        // lazily access the statusBarColor in case the value changes between when this
        // class is instantiated and the payment sheet is launched
        { getStatusBarColor(fragment.activity) }
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: PaymentSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            PaymentSheetContract(),
            registry
        ) {
            callback.onPaymentResult(it)
        },

        // lazily access the statusBarColor in case the value changes between when this
        // class is instantiated and the payment sheet is launched
        { getStatusBarColor(fragment.activity) }
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

    private companion object {
        private fun getStatusBarColor(activity: Activity?) = activity?.window?.statusBarColor
    }
}

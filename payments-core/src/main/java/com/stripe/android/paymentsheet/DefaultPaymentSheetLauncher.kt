package com.stripe.android.paymentsheet

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import org.jetbrains.annotations.TestOnly

internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContract.Args>,
    private val statusBarColor: () -> Int?
) : PaymentSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onPaymentSheetResult(it)
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
            callback.onPaymentSheetResult(it)
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
            callback.onPaymentSheetResult(it)
        },

        // lazily access the statusBarColor in case the value changes between when this
        // class is instantiated and the payment sheet is launched
        { getStatusBarColor(fragment.activity) }
    )

    override fun presentWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?
    ) = present(
        PaymentSheetContract.Args(
            PaymentIntentClientSecret(paymentIntentClientSecret),
            statusBarColor(),
            configuration
        )
    )

    override fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?
    ) = present(
        PaymentSheetContract.Args(
            SetupIntentClientSecret(setupIntentClientSecret),
            statusBarColor(),
            configuration
        )
    )

    private fun present(args: PaymentSheetContract.Args) {
        activityResultLauncher.launch(args)
    }

    private companion object {
        private fun getStatusBarColor(activity: Activity?) = activity?.window?.statusBarColor
    }
}

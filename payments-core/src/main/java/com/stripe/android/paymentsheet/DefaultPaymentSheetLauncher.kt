package com.stripe.android.paymentsheet

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import org.jetbrains.annotations.TestOnly

/**
 * This is used internally for integrations that don't use Jetpack Compose and are
 * able to pass in an activity.
 */
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
        PaymentSheetContract.Args.createPaymentIntentArgs(
            paymentIntentClientSecret,
            configuration,
            statusBarColor(),
        )
    )

    override fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?
    ) = present(
        PaymentSheetContract.Args.createSetupIntentArgs(
            setupIntentClientSecret,
            configuration,
            statusBarColor(),
        )
    )

    private fun present(args: PaymentSheetContract.Args) {
        activityResultLauncher.launch(args)
    }

    private companion object {
        private fun getStatusBarColor(activity: Activity?) = activity?.window?.statusBarColor
    }
}

package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import org.jetbrains.annotations.TestOnly

/**
 * This is used internally for integrations that don't use Jetpack Compose and are
 * able to pass in an activity.
 */
internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContract.Args>,
) : PaymentSheetLauncher {

    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onPaymentSheetResult(it)
        }
    )

    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onPaymentSheetResult(it)
        }
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
        }
    )

    override fun presentWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?
    ) = present(
        PaymentSheetContract.Args.createPaymentIntentArgs(
            paymentIntentClientSecret,
            configuration
        )
    )

    override fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?
    ) = present(
        PaymentSheetContract.Args.createSetupIntentArgs(
            setupIntentClientSecret,
            configuration
        )
    )

    private fun present(args: PaymentSheetContract.Args) {
        activityResultLauncher.launch(args)
    }
}

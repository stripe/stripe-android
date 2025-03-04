package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

@Suppress("DEPRECATION")
@Deprecated(
    message = "This isn't meant for public usage and will be removed in a future " +
        "release. If you're looking to integrate with PaymentSheet in Compose, " +
        "use rememberPaymentSheet() instead.",
)
class PaymentSheetContract :
    ActivityResultContract<PaymentSheetContract.Args, PaymentSheetResult>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        val statusBarColor = (context as? Activity)?.window?.statusBarColor
        val args = input.copy(statusBarColor = statusBarColor)
        return Intent(context, PaymentSheetActivity::class.java).putExtra(EXTRA_ARGS, args.toV2(context))
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentSheetResult {
        @Suppress("DEPRECATION")
        val paymentResult = intent?.getParcelableExtra<PaymentSheetContractV2.Result>(EXTRA_RESULT)?.paymentSheetResult
        return paymentResult ?: PaymentSheetResult.Failed(
            IllegalArgumentException("Failed to retrieve a PaymentSheetResult.")
        )
    }

    @Parcelize
    data class Args internal constructor(
        internal val clientSecret: ClientSecret,
        internal val config: PaymentSheet.Configuration?,
        internal val paymentElementCallbackIdentifier: String,
        @ColorInt internal val statusBarColor: Int? = null,
        @InjectorKey internal val injectorKey: String = DUMMY_INJECTOR_KEY
    ) : ActivityStarter.Args {

        val googlePayConfig: PaymentSheet.GooglePayConfiguration? get() = config?.googlePay

        internal fun toV2(context: Context): PaymentSheetContractV2.Args {
            return PaymentSheetContractV2.Args(
                initializationMode = when (clientSecret) {
                    is PaymentIntentClientSecret -> {
                        PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret.value)
                    }
                    is SetupIntentClientSecret -> {
                        PaymentElementLoader.InitializationMode.SetupIntent(clientSecret.value)
                    }
                },
                config = config ?: PaymentSheet.Configuration.default(context),
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                statusBarColor = statusBarColor,
                initializedViaCompose = false,
            )
        }

        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }

            fun createPaymentIntentArgs(
                clientSecret: String,
                config: PaymentSheet.Configuration? = null
            ) = Args(
                clientSecret = PaymentIntentClientSecret(clientSecret),
                paymentElementCallbackIdentifier = PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER,
                config = config,
            )

            fun createSetupIntentArgs(
                clientSecret: String,
                config: PaymentSheet.Configuration? = null
            ) = Args(
                clientSecret = SetupIntentClientSecret(clientSecret),
                paymentElementCallbackIdentifier = PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER,
                config = config,
            )
        }
    }

    @Parcelize
    internal data class Result(
        val paymentSheetResult: PaymentSheetResult
    ) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EXTRA_ARGS =
            "com.stripe.android.paymentsheet.PaymentSheetContract.extra_args"
        private const val EXTRA_RESULT =
            "com.stripe.android.paymentsheet.PaymentSheetContract.extra_result"
    }
}

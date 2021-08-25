package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

class PaymentSheetContract :
    ActivityResultContract<PaymentSheetContract.Args, PaymentSheetResult>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        val statusBarColor = (context as? Activity)?.window?.statusBarColor
        return Intent(context, PaymentSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input.copy(statusBarColor = statusBarColor))
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentSheetResult {
        val paymentResult = intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.paymentSheetResult
        return paymentResult ?: PaymentSheetResult.Failed(
            IllegalArgumentException("Failed to retrieve a PaymentSheetResult.")
        )
    }

    @Parcelize
    data class Args @VisibleForTesting internal constructor(
        internal val clientSecret: ClientSecret,
        internal val config: PaymentSheet.Configuration?,
        @ColorInt internal val statusBarColor: Int? = null,
    ) : ActivityStarter.Args {
        val googlePayConfig: PaymentSheet.GooglePayConfiguration? get() = config?.googlePay

        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }

            fun createPaymentIntentArgs(
                clientSecret: String,
                config: PaymentSheet.Configuration? = null
            ) = Args(
                PaymentIntentClientSecret(clientSecret),
                config
            )

            fun createSetupIntentArgs(
                clientSecret: String,
                config: PaymentSheet.Configuration? = null
            ) = Args(
                SetupIntentClientSecret(clientSecret),
                config
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

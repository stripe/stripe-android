package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentSheetContractV2 :
    ActivityResultContract<PaymentSheetContractV2.Args, PaymentSheetResult>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, PaymentSheetActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentSheetResult {
        @Suppress("DEPRECATION")
        val paymentResult = intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.paymentSheetResult
        return paymentResult ?: PaymentSheetResult.Failed(
            IllegalArgumentException("Failed to retrieve a PaymentSheetResult.")
        )
    }

    @Parcelize
    data class Args(
        internal val initializationMode: PaymentElementLoader.InitializationMode,
        internal val config: PaymentSheet.Configuration,
        internal val paymentElementCallbackIdentifier: String,
        @ColorInt internal val statusBarColor: Int?,
        val initializedViaCompose: Boolean = false,
    ) : ActivityStarter.Args {

        val googlePayConfig: PaymentSheet.GooglePayConfiguration? get() = config.googlePay

        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
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

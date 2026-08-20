package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityArgs
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentSheetContract :
    ActivityResultContract<PaymentSheetContract.Args, PaymentSheetResult>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, EmbeddedSheetActivity::class.java).putExtra(
            SheetActivityArgs.EXTRA_ARGS,
            SheetActivityArgs.PaymentSheet(input),
        )
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
    }

    @Parcelize
    internal data class Result(
        val paymentSheetResult: PaymentSheetResult
    ) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    private companion object {
        private const val EXTRA_RESULT =
            "com.stripe.android.paymentsheet.PaymentSheetContract.extra_result"
    }
}

package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentSheetContract :
    ActivityResultContract<PaymentSheetContract.Args, PaymentResult>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, PaymentSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentResult {
        val paymentResult = intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.paymentResult
        return paymentResult ?: PaymentResult.Failed(
            IllegalArgumentException("Failed to retrieve a PaymentResult."),
            null
        )
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val sessionId: SessionId,
        @ColorInt val statusBarColor: Int?,
        val config: PaymentSheet.Configuration?
    ) : ActivityStarter.Args {
        val googlePayConfig: PaymentSheet.GooglePayConfiguration? get() = config?.googlePay
        val isGooglePayEnabled: Boolean get() = googlePayConfig != null

        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    internal data class Result(
        val paymentResult: PaymentResult
    ) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    private companion object {
        private const val EXTRA_ARGS =
            "com.stripe.android.paymentsheet.PaymentSheetContract.extra_args"
        private const val EXTRA_RESULT =
            "com.stripe.android.paymentsheet.PaymentSheetContract.extra_result"
    }
}

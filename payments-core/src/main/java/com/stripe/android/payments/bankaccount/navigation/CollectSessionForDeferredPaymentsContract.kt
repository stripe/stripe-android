package com.stripe.android.payments.bankaccount.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.stripe.android.payments.bankaccount.ui.CollectSessionForDeferredPaymentsActivity
import kotlinx.parcelize.Parcelize

internal class CollectSessionForDeferredPaymentsContract :
    ActivityResultContract<CollectSessionForDeferredPaymentsContract.Args, CollectSessionForDeferredPaymentsResult>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, CollectSessionForDeferredPaymentsActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): CollectSessionForDeferredPaymentsResult {
        val result =
            intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.collectSessionForDeferredPaymentsResult
        return result ?: CollectSessionForDeferredPaymentsResult.Failed(
            IllegalArgumentException("Failed to retrieve a session.")
        )
    }

    @Parcelize
    internal data class Args(
        val publishableKey: String,
        val stripeAccountId: String?,
        val elementsSessionId: String,
        val customer: String?,
        val amount: Int?,
        val currency: String?
    ) : Parcelable {
        internal companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    internal data class Result(
        val collectSessionForDeferredPaymentsResult: CollectSessionForDeferredPaymentsResult
    ) : Parcelable {
        fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EXTRA_ARGS =
            "com.stripe.android.payments.bankaccount.navigation.CreateFinancialConnectionsSessionContract.extra_args"
        private const val EXTRA_RESULT =
            "com.stripe.android.payments.bankaccount.navigation.CreateFinancialConnectionsSessionContract.extra_result"
    }
}

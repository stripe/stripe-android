package com.stripe.android.payments.bankaccount.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity
import kotlinx.parcelize.Parcelize

internal class CollectBankAccountContract :
    ActivityResultContract<CollectBankAccountContract.Args, CollectBankAccountResult>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, CollectBankAccountActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): CollectBankAccountResult {
        val result =
            intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.collectBankAccountResult
        return result ?: CollectBankAccountResult.Failed(
            IllegalArgumentException("Failed to retrieve a CollectBankAccountResult.")
        )
    }

    sealed class Args(
        open val publishableKey: String,
        open val clientSecret: String,
        open val params: CollectBankAccountConfiguration,
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        data class ForPaymentIntent internal constructor(
            override val publishableKey: String,
            override val clientSecret: String,
            override val params: CollectBankAccountConfiguration,
        ) : Args(publishableKey, clientSecret, params)

        @Parcelize
        data class ForSetupIntent internal constructor(
            override val publishableKey: String,
            override val clientSecret: String,
            override val params: CollectBankAccountConfiguration,
        ) : Args(publishableKey, clientSecret, params)

        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    internal data class Result(
        val collectBankAccountResult: CollectBankAccountResult
    ) : Parcelable {
        fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EXTRA_ARGS =
            "com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.extra_args"
        private const val EXTRA_RESULT =
            "com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.extra_result"
    }
}

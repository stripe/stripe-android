package com.stripe.android.payments.bankaccount.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CollectBankAccountContract :
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

    /**
     * @param attachToIntent enable this to attach the link account session to the given intent
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Args(
        open val publishableKey: String,
        open val clientSecret: String,
        open val configuration: CollectBankAccountConfiguration,
        open val attachToIntent: Boolean
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class ForPaymentIntent constructor(
            override val publishableKey: String,
            override val clientSecret: String,
            override val configuration: CollectBankAccountConfiguration,
            override val attachToIntent: Boolean
        ) : Args(publishableKey, clientSecret, configuration, attachToIntent)

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class ForSetupIntent constructor(
            override val publishableKey: String,
            override val clientSecret: String,
            override val configuration: CollectBankAccountConfiguration,
            override val attachToIntent: Boolean
        ) : Args(publishableKey, clientSecret, configuration, attachToIntent)

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

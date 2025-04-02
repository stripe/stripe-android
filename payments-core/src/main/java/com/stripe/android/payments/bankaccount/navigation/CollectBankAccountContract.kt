package com.stripe.android.payments.bankaccount.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.stripe.android.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CollectBankAccountContract :
    ActivityResultContract<CollectBankAccountContract.Args, CollectBankAccountResultInternal>() {

    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, CollectBankAccountActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): CollectBankAccountResultInternal {
        val result =
            intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.collectBankAccountResult
        return result ?: CollectBankAccountResultInternal.Failed(
            IllegalArgumentException("Failed to retrieve a CollectBankAccountResult.")
        )
    }

    /**
     * @param attachToIntent enable this to attach the link account session to the given intent
     * @param clientSecret the client secret of the StripeIntent, null when running in the deferred
     * intent flow.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Args(
        open val publishableKey: String,
        open val stripeAccountId: String?,
        open val clientSecret: String?,
        open val configuration: CollectBankAccountConfiguration,
        open val attachToIntent: Boolean,
        open val financialConnectionsAvailability: FinancialConnectionsAvailability?,
        open val hostedSurface: String?
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        val product: String?
            get() = when (configuration) {
                is CollectBankAccountConfiguration.InstantDebits -> "instant_debits"
                is CollectBankAccountConfiguration.USBankAccountInternal,
                is CollectBankAccountConfiguration.USBankAccount -> null
            }

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class ForPaymentIntent(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val clientSecret: String,
            override val configuration: CollectBankAccountConfiguration,
            override val attachToIntent: Boolean,
            override val financialConnectionsAvailability: FinancialConnectionsAvailability?,
            override val hostedSurface: String?
        ) : Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            hostedSurface = hostedSurface,
            clientSecret = clientSecret,
            configuration = configuration,
            financialConnectionsAvailability = financialConnectionsAvailability,
            attachToIntent = attachToIntent
        )

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class ForSetupIntent(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val clientSecret: String,
            override val configuration: CollectBankAccountConfiguration,
            override val attachToIntent: Boolean,
            override val financialConnectionsAvailability: FinancialConnectionsAvailability?,
            override val hostedSurface: String?
        ) : Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            hostedSurface = hostedSurface,
            clientSecret = clientSecret,
            configuration = configuration,
            financialConnectionsAvailability = financialConnectionsAvailability,
            attachToIntent = attachToIntent
        )

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class ForDeferredPaymentIntent(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val configuration: CollectBankAccountConfiguration,
            override val financialConnectionsAvailability: FinancialConnectionsAvailability?,
            override val hostedSurface: String?,
            val elementsSessionId: String,
            val customerId: String?,
            val onBehalfOf: String?,
            val amount: Int?,
            val currency: String?,
        ) : Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            hostedSurface = hostedSurface,
            clientSecret = null,
            configuration = configuration,
            financialConnectionsAvailability = financialConnectionsAvailability,
            attachToIntent = false,
        )

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class ForDeferredSetupIntent(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val configuration: CollectBankAccountConfiguration,
            override val hostedSurface: String?,
            override val financialConnectionsAvailability: FinancialConnectionsAvailability?,
            val elementsSessionId: String,
            val customerId: String?,
            val onBehalfOf: String?,
        ) : Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            hostedSurface = hostedSurface,
            clientSecret = null,
            configuration = configuration,
            financialConnectionsAvailability = financialConnectionsAvailability,
            attachToIntent = false
        )

        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Result(
        val collectBankAccountResult: CollectBankAccountResultInternal
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

package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability

internal class CollectBankAccountForACHLauncher(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
    private val hostedSurface: String?,
    private val financialConnectionsAvailability: FinancialConnectionsAvailability?
) : CollectBankAccountLauncher {

    private val attachToIntent: Boolean
        // We only attach the intent if we're not hosted within another
        // Stripe surface. If we're in one, then the surface will take care of
        // attaching the LinkAccountSession.
        get() = hostedSurface == null

    override fun presentWithPaymentIntent(
        publishableKey: String,
        stripeAccountId: String?,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                clientSecret = clientSecret,
                configuration = configuration,
                hostedSurface = hostedSurface,
                attachToIntent = attachToIntent,
                financialConnectionsAvailability = financialConnectionsAvailability
            )
        )
    }

    override fun presentWithSetupIntent(
        publishableKey: String,
        stripeAccountId: String?,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                clientSecret = clientSecret,
                configuration = configuration,
                hostedSurface = hostedSurface,
                attachToIntent = attachToIntent,
                financialConnectionsAvailability = financialConnectionsAvailability
            )
        )
    }

    override fun presentWithDeferredPayment(
        publishableKey: String,
        stripeAccountId: String?,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
        amount: Int?,
        currency: String?
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForDeferredPaymentIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                elementsSessionId = elementsSessionId,
                configuration = configuration,
                customerId = customerId,
                onBehalfOf = onBehalfOf,
                amount = amount,
                hostedSurface = hostedSurface,
                financialConnectionsAvailability = financialConnectionsAvailability,
                currency = currency,
            )
        )
    }

    override fun presentWithDeferredSetup(
        publishableKey: String,
        stripeAccountId: String?,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForDeferredSetupIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                elementsSessionId = elementsSessionId,
                configuration = configuration,
                customerId = customerId,
                hostedSurface = hostedSurface,
                financialConnectionsAvailability = financialConnectionsAvailability,
                onBehalfOf = onBehalfOf,
            )
        )
    }

    override fun unregister() {
        hostActivityLauncher.unregister()
    }
}

package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.financialconnections.FinancialConnectionsMode
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract

internal class CollectBankAccountForACHLauncher(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
    private val hostedSurface: String?,
    private val financialConnectionsMode: FinancialConnectionsMode
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
                financialConnectionsMode = financialConnectionsMode
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
                financialConnectionsMode = financialConnectionsMode
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
                financialConnectionsMode = financialConnectionsMode,
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
                financialConnectionsMode = financialConnectionsMode,
                onBehalfOf = onBehalfOf,
            )
        )
    }

    override fun unregister() {
        hostActivityLauncher.unregister()
    }
}

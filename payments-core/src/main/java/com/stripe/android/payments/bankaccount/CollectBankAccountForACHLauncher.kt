package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsPreCollectedConsent
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
    ) = presentWithPaymentIntent(
        publishableKey = publishableKey,
        stripeAccountId = stripeAccountId,
        clientSecret = clientSecret,
        configuration = configuration,
        preCollectedConsent = null,
    )

    override fun presentWithPaymentIntent(
        publishableKey: String,
        stripeAccountId: String?,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) = presentWithPaymentIntent(
        apiConfiguration = ApiConfiguration.State(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
        ),
        clientSecret = clientSecret,
        configuration = configuration,
        preCollectedConsent = preCollectedConsent,
    )

    override fun presentWithPaymentIntent(
        apiConfiguration: ApiConfiguration.State,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                apiConfiguration = apiConfiguration,
                clientSecret = clientSecret,
                configuration = configuration,
                hostedSurface = hostedSurface,
                attachToIntent = attachToIntent,
                financialConnectionsAvailability = financialConnectionsAvailability,
                preCollectedConsent = preCollectedConsent.takeIf { hostedSurface == null }
            )
        )
    }

    override fun presentWithSetupIntent(
        publishableKey: String,
        stripeAccountId: String?,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration
    ) = presentWithSetupIntent(
        publishableKey = publishableKey,
        stripeAccountId = stripeAccountId,
        clientSecret = clientSecret,
        configuration = configuration,
        preCollectedConsent = null,
    )

    override fun presentWithSetupIntent(
        publishableKey: String,
        stripeAccountId: String?,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) = presentWithSetupIntent(
        apiConfiguration = ApiConfiguration.State(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
        ),
        clientSecret = clientSecret,
        configuration = configuration,
        preCollectedConsent = preCollectedConsent,
    )

    override fun presentWithSetupIntent(
        apiConfiguration: ApiConfiguration.State,
        clientSecret: String,
        configuration: CollectBankAccountConfiguration,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                apiConfiguration = apiConfiguration,
                clientSecret = clientSecret,
                configuration = configuration,
                hostedSurface = hostedSurface,
                attachToIntent = attachToIntent,
                financialConnectionsAvailability = financialConnectionsAvailability,
                preCollectedConsent = preCollectedConsent.takeIf { hostedSurface == null }
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
        currency: String?,
    ) = presentWithDeferredPayment(
        apiConfiguration = ApiConfiguration.State(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
        ),
        configuration = configuration,
        elementsSessionId = elementsSessionId,
        customerId = customerId,
        onBehalfOf = onBehalfOf,
        amount = amount,
        currency = currency,
    )

    override fun presentWithDeferredPayment(
        apiConfiguration: ApiConfiguration.State,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
        amount: Int?,
        currency: String?
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForDeferredPaymentIntent(
                apiConfiguration = apiConfiguration,
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
    ) = presentWithDeferredSetup(
        apiConfiguration = ApiConfiguration.State(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
        ),
        configuration = configuration,
        elementsSessionId = elementsSessionId,
        customerId = customerId,
        onBehalfOf = onBehalfOf,
    )

    override fun presentWithDeferredSetup(
        apiConfiguration: ApiConfiguration.State,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
    ) {
        hostActivityLauncher.launch(
            CollectBankAccountContract.Args.ForDeferredSetupIntent(
                apiConfiguration = apiConfiguration,
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

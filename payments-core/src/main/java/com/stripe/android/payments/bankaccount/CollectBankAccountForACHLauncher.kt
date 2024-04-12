package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract

internal class CollectBankAccountForACHLauncher(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>
) : CollectBankAccountLauncher {

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
                attachToIntent = true
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
                attachToIntent = true
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
                onBehalfOf = onBehalfOf,
            )
        )
    }

    override fun unregister() {
        hostActivityLauncher.unregister()
    }
}

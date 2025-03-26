package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountForInstantDebitsResult
import com.stripe.android.payments.bankaccount.navigation.toInstantDebitsResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CollectBankAccountForInstantDebitsLauncher(
    private val hostActivityLauncher: ActivityResultLauncher<CollectBankAccountContract.Args>,
    private val financialConnectionsAvailability: FinancialConnectionsAvailability?,
    private val hostedSurface: String?,
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
                hostedSurface = hostedSurface,
                financialConnectionsAvailability = financialConnectionsAvailability,
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
                hostedSurface = hostedSurface,
                financialConnectionsAvailability = financialConnectionsAvailability,
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
                onBehalfOf = onBehalfOf,
                financialConnectionsAvailability = financialConnectionsAvailability,
                hostedSurface = hostedSurface,
            )
        )
    }

    override fun unregister() {
        hostActivityLauncher.unregister()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        private const val LAUNCHER_KEY = "CollectBankAccountForInstantDebitsLauncher"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun createForPaymentSheet(
            hostedSurface: String,
            financialConnectionsAvailability: FinancialConnectionsAvailability?,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            callback: (CollectBankAccountForInstantDebitsResult) -> Unit,
        ): CollectBankAccountLauncher {
            return CollectBankAccountForInstantDebitsLauncher(
                // TODO@carlosmuvi: if exposing this as an L1 (standalone) integration,
                // use a separate method and ensure the correct hostedSurface is set.
                hostedSurface = hostedSurface,
                financialConnectionsAvailability = financialConnectionsAvailability,
                hostActivityLauncher = activityResultRegistryOwner.activityResultRegistry.register(
                    LAUNCHER_KEY,
                    CollectBankAccountContract()
                ) {
                    callback(it.toInstantDebitsResult())
                }
            )
        }
    }
}

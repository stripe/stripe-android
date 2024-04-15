package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountForInstantDebitsResult
import com.stripe.android.payments.bankaccount.navigation.toInstantDebitsResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CollectBankAccountForInstantDebitsLauncher(
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
        TODO("Instant Debits does not support deferred payments yet")
    }

    override fun presentWithDeferredSetup(
        publishableKey: String,
        stripeAccountId: String?,
        configuration: CollectBankAccountConfiguration,
        elementsSessionId: String,
        customerId: String?,
        onBehalfOf: String?,
    ) {
        TODO("Instant Debits does not support deferred payments yet")
    }

    override fun unregister() {
        hostActivityLauncher.unregister()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        private const val LAUNCHER_KEY = "CollectBankAccountForInstantDebitsLauncher"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            callback: (CollectBankAccountForInstantDebitsResult) -> Unit,
        ): CollectBankAccountLauncher {
            return CollectBankAccountForInstantDebitsLauncher(
                activityResultRegistryOwner.activityResultRegistry.register(
                    LAUNCHER_KEY,
                    CollectBankAccountContract()
                ) {
                    callback(it.toInstantDebitsResult())
                }
            )
        }
    }
}

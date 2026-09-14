package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.createPaymentMethod
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.Merchant

internal class CheckoutControllerExampleBackendRepository(
    private val applicationContext: Context,
) {
    private val defaultBackendUrl = Settings(applicationContext).playgroundBackendUrl

    suspend fun fetchCheckoutSession(
        settings: CheckoutPlaygroundSettings.Snapshot,
        backendUrl: String?,
    ): Result<CheckoutControllerExampleBackendResponse> = runCatching {
        val merchant = settings.backendMerchant()
        val backend = PlaygroundBackend(
            baseUrl = backendUrl ?: defaultBackendUrl,
            merchant = merchant.value,
        )
        val publishableKey = backend.fetchPublishableKey()
        PaymentConfiguration.init(applicationContext, publishableKey)
        val stripe = Stripe(applicationContext, publishableKey)
        CheckoutSessionFactory(
            backend = backend,
            paymentMethodCreator = PlaygroundPaymentMethodCreator { params ->
                stripe.createPaymentMethod(params).id
            },
        ).create(settings)
    }
}

internal fun CheckoutPlaygroundSettings.Snapshot.backendMerchant(): Merchant {
    val session = CheckoutPlaygroundDefinitions.session
    return if (this[session.automaticTax]) Merchant.US_TAX else this[session.merchant]
}

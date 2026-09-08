package com.stripe.android.paymentsheet.example.playground.checkout

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class CheckoutSessionFactory(
    private val backend: CheckoutPlaygroundBackend,
    private val paymentMethodCreator: PlaygroundPaymentMethodCreator,
) {
    suspend fun create(settings: CheckoutPlaygroundSettings.Snapshot): CheckoutControllerExampleBackendResponse {
        val session = CheckoutPlaygroundDefinitions.session
        val customerId = when (settings[session.customer]) {
            CheckoutCustomer.Guest -> null
            CheckoutCustomer.New -> createCustomer(settings)
            CheckoutCustomer.Returning -> settings[session.customerId] ?: createReturningCustomer(settings)
        }
        val params = CheckoutSessionParamsFactory.create(settings, customerId)
        return CheckoutControllerExampleBackendResponse(
            clientSecret = backend.createCheckoutSession(params),
            customerId = customerId,
        )
    }

    private suspend fun createReturningCustomer(settings: CheckoutPlaygroundSettings.Snapshot): String {
        val customerId = createCustomer(settings)
        val paymentMethodId = paymentMethodCreator.createPaymentMethod(TEST_CARD_PARAMS)
        backend.attachPaymentMethod(paymentMethodId, customerId)
        return customerId
    }

    private suspend fun createCustomer(settings: CheckoutPlaygroundSettings.Snapshot): String {
        val email = CheckoutSessionParamsFactory.resolvedEmail(settings)
        return backend.createCustomer(
            buildJsonObject {
                email?.let { put("email", it) }
            },
        )
    }

    private companion object {
        const val TEST_CARD_EXPIRY_MONTH = 12
        const val TEST_CARD_EXPIRY_YEAR = 2030

        val TEST_CARD_PARAMS = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card.Builder()
                .setNumber("4242424242424242")
                .setExpiryMonth(TEST_CARD_EXPIRY_MONTH)
                .setExpiryYear(TEST_CARD_EXPIRY_YEAR)
                .setCvc("123")
                .build(),
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    city = "South San Francisco",
                    country = "US",
                    line1 = "354 Oyster Point Blvd",
                    postalCode = "94080",
                    state = "CA",
                ),
                email = "jenny.rosen@example.com",
                name = "Jenny Rosen",
                phone = "+15555555555",
            ),
            metadata = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        )
    }
}

internal fun interface PlaygroundPaymentMethodCreator {
    suspend fun createPaymentMethod(params: PaymentMethodCreateParams): String
}

internal data class CheckoutControllerExampleBackendResponse(
    val clientSecret: String,
    val customerId: String?,
)

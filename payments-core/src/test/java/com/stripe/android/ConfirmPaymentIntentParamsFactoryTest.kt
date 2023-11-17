package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryTest {

    private val factory = ConfirmPaymentIntentParamsFactory(
        clientSecret = CLIENT_SECRET,
        shipping = null,
    )

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = OffSession`() {
        assertThat(
            factory.create(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        )
    }

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = blank`() {
        assertThat(
            factory.create(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                )
            )
        )
    }

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = null`() {
        assertThat(
            factory.create(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = PaymentMethodOptionsParams.Card(),
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card()
            )
        )
    }

    @Test
    fun `create() with saved card and shippingDetails sets shipping field`() {
        val shippingDetails = ConfirmPaymentIntentParams.Shipping(
            name = "Test",
            address = Address(
                line1 = "line1",
                city = "city",
            ),
            phone = "5555555555",
        )

        val factoryWithConfig = ConfirmPaymentIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            shipping = shippingDetails,
        )

        val result = factoryWithConfig.create(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertThat(result.shipping).isEqualTo(shippingDetails)
    }

    @Test
    fun `create() with new card and shippingDetails sets shipping field`() {
        val shippingDetails = ConfirmPaymentIntentParams.Shipping(
            name = "Test",
            address = Address(
                line1 = "line1",
                city = "city",
            ),
            phone = "5555555555",
        )

        val factoryWithConfig = ConfirmPaymentIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            shipping = shippingDetails,
        )

        val result = factoryWithConfig.create(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
        )

        assertThat(result.shipping).isEqualTo(shippingDetails)
    }

    private companion object {
        private const val CLIENT_SECRET = "client_secret"
    }
}

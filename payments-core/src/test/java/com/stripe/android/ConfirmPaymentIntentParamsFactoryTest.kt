package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryTest {

    private val factory = ConfirmPaymentIntentParamsFactory(
        clientSecret = CLIENT_SECRET,
        intent = createPaymentIntent(),
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
                setAsDefaultPaymentMethod = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card()
            )
        )
    }

    @Test
    fun `create() with new card when setAsDefaultPaymentMethod is true`() {
        val paymentIntentParams = factory.create(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = true
            )
        )
        assertThat(paymentIntentParams.setAsDefaultPaymentMethod).isEqualTo(true)
    }


    @Test
    fun `create() with new card when setAsDefaultPaymentMethod is false`() {
        val paymentIntentParams = factory.create(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = false
            )
        )
        assertThat(paymentIntentParams.setAsDefaultPaymentMethod).isEqualTo(false)
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
            intent = createPaymentIntent(),
            shipping = shippingDetails,
        )

        val result = factoryWithConfig.create(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
            extraParams = null
        )
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
            intent = createPaymentIntent(),
            shipping = shippingDetails,
        )

        val result = factoryWithConfig.create(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
        )

        assertThat(result.shipping).isEqualTo(shippingDetails)
    }

    @Test
    fun `create() with saved card and does not require save on confirmation`() {
        val factoryWithConfig = ConfirmPaymentIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = createPaymentIntent(),
            shipping = null,
        )

        val result = factoryWithConfig.create(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
            ),
            extraParams = null,
        )

        assertThat(result.paymentMethodOptions).isEqualTo(
            PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
            )
        )
    }

    @Test
    fun `create() with saved card and requires save on confirmation`() {
        val factoryWithConfig = ConfirmPaymentIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = createPaymentIntent(),
            shipping = null,
        )

        val result = factoryWithConfig.create(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            extraParams = null,
        )

        assertThat(result.paymentMethodOptions).isEqualTo(
            PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            )
        )
    }

    @Test
    fun `create() without SFU should not contain any mandate data`() = mandateDataTest(
        setupFutureUsage = null,
        expectedMandateDataParams = null,
    )

    @Test
    fun `create() without SFU should contain mandate data for sepa_debit`() = mandateDataTest(
        setupFutureUsage = null,
        expectedMandateDataParams = MandateDataParams(MandateDataParams.Type.Online.DEFAULT),
        paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
    )

    @Test
    fun `create() with 'OneTime' SFU should not contain any mandate data`() = mandateDataTest(
        setupFutureUsage = StripeIntent.Usage.OneTime,
        expectedMandateDataParams = null,
    )

    @Test
    fun `create() with 'OnSession' SFU should contain any mandate data`() = mandateDataTest(
        setupFutureUsage = StripeIntent.Usage.OnSession,
        expectedMandateDataParams = MandateDataParams(MandateDataParams.Type.Online.DEFAULT),
    )

    @Test
    fun `create() with 'OffSession' SFU should contain any mandate data`() = mandateDataTest(
        setupFutureUsage = StripeIntent.Usage.OffSession,
        expectedMandateDataParams = MandateDataParams(MandateDataParams.Type.Online.DEFAULT),
    )

    private fun mandateDataTest(
        setupFutureUsage: StripeIntent.Usage?,
        expectedMandateDataParams: MandateDataParams?,
        paymentMethod: PaymentMethod = PaymentMethodFactory.cashAppPay(),
    ) {
        val factoryWithConfig = ConfirmPaymentIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = createPaymentIntent(
                setupFutureUsage = setupFutureUsage,
            ),
            shipping = null,
        )

        val result = factoryWithConfig.create(
            paymentMethod = paymentMethod,
            optionsParams = null,
            extraParams = null,
        )

        assertThat(result).isInstanceOf(ConfirmPaymentIntentParams::class.java)

        val params = result.asConfirmPaymentIntentParams()

        assertThat(params.mandateData).isEqualTo(expectedMandateDataParams)
    }

    private fun createPaymentIntent(
        setupFutureUsage: StripeIntent.Usage? = null,
    ): PaymentIntent {
        return PaymentIntentFactory.create(
            setupFutureUsage = setupFutureUsage,
        )
    }

    private fun ConfirmStripeIntentParams.asConfirmPaymentIntentParams(): ConfirmPaymentIntentParams {
        return this as ConfirmPaymentIntentParams
    }

    private companion object {
        private const val CLIENT_SECRET = "client_secret"
    }
}

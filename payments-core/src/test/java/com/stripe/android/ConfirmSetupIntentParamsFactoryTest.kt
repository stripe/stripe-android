package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import org.junit.Test

class ConfirmSetupIntentParamsFactoryTest {
    @Test
    fun `create() should contain mandate data`() {
        val result = getConfirmSetupIntentParamsForTesting()
        assertThat(result).isInstanceOf(ConfirmSetupIntentParams::class.java)

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.mandateData).isEqualTo(MandateDataParams(MandateDataParams.Type.Online.DEFAULT))
    }

    @Test
    fun `create() should set setAsDefault as true when using create params`() {
        val result = getConfirmSetupIntentParamsForTesting(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = true
            )
        )

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.setAsDefaultPaymentMethod).isTrue()
    }

    @Test
    fun `create() should set setAsDefault as false when using create params`() {
        val result = getConfirmSetupIntentParamsForTesting(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = false
            )
        )

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.setAsDefaultPaymentMethod).isFalse()
    }

    @Test
    fun `create() should set setAsDefault as true when using payment method`() {
        val result = getConfirmSetupIntentParamsForTesting(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = true
            )
        )

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.setAsDefaultPaymentMethod).isTrue()
    }

    @Test
    fun `create() should set setAsDefault as false when using payment method`() {
        val result = getConfirmSetupIntentParamsForTesting(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = false
            )
        )

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.setAsDefaultPaymentMethod).isFalse()
    }

    private fun getConfirmSetupIntentParamsForTesting(): ConfirmSetupIntentParams {
        val factoryWithConfig = ConfirmSetupIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = SetupIntentFactory.create(),
        )

        return factoryWithConfig.create(
            paymentMethod = PaymentMethodFactory.cashAppPay(),
            optionsParams = null,
            extraParams = null,
        )
    }

    private fun getConfirmSetupIntentParamsForTesting(
        paymentMethod: PaymentMethod,
        extraParams: PaymentMethodExtraParams
    ): ConfirmSetupIntentParams {
        val factoryWithConfig = ConfirmSetupIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = SetupIntentFactory.create(),
        )

        return factoryWithConfig.create(
            paymentMethod = paymentMethod,
            optionsParams = null,
            extraParams = extraParams,
        )
    }

    private fun getConfirmSetupIntentParamsForTesting(
        createParams: PaymentMethodCreateParams,
        extraParams: PaymentMethodExtraParams
    ): ConfirmSetupIntentParams {
        val factoryWithConfig = ConfirmSetupIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = SetupIntentFactory.create(),
        )

        return factoryWithConfig.create(
            createParams = createParams,
            optionsParams = null,
            extraParams = extraParams,
        )
    }

    private fun ConfirmStripeIntentParams.asConfirmSetupIntentParams(): ConfirmSetupIntentParams {
        return this as ConfirmSetupIntentParams
    }

    private companion object {
        private const val CLIENT_SECRET = "client_secret"
    }
}

package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodExtraParams
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
    fun `create() should set setAsDefault as true`() {
        val result = getConfirmSetupIntentParamsForTesting(
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = true
            )
        )

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.setAsDefaultPaymentMethod).isEqualTo(true)
    }

    @Test
    fun `create() should set setAsDefault as false`() {
        val result = getConfirmSetupIntentParamsForTesting(
            extraParams = PaymentMethodExtraParams.Card(
                setAsDefault = false
            )
        )

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.setAsDefaultPaymentMethod).isEqualTo(false)
    }

    private fun getConfirmSetupIntentParamsForTesting(
        extraParams: PaymentMethodExtraParams? = null
    ) : ConfirmSetupIntentParams {
        val factoryWithConfig = ConfirmSetupIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = SetupIntentFactory.create(),
        )

        return factoryWithConfig.create(
            paymentMethod = PaymentMethodFactory.cashAppPay(),
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

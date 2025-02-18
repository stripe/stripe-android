package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import org.junit.Test

class ConfirmSetupIntentParamsFactoryTest {
    @Test
    fun `create() should contain mandate data`() {
        val factoryWithConfig = ConfirmSetupIntentParamsFactory(
            clientSecret = CLIENT_SECRET,
            intent = SetupIntentFactory.create(),
        )

        val result = factoryWithConfig.create(
            paymentMethod = PaymentMethodFactory.cashAppPay(),
            optionsParams = null,
        )

        assertThat(result).isInstanceOf(ConfirmSetupIntentParams::class.java)

        val params = result.asConfirmSetupIntentParams()

        assertThat(params.mandateData).isEqualTo(MandateDataParams(MandateDataParams.Type.Online.DEFAULT))
    }

    private fun ConfirmStripeIntentParams.asConfirmSetupIntentParams(): ConfirmSetupIntentParams {
        return this as ConfirmSetupIntentParams
    }

    private companion object {
        private const val CLIENT_SECRET = "client_secret"
    }
}

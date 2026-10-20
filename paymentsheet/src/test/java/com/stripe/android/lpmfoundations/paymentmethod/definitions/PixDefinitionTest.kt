package com.stripe.android.lpmfoundations.paymentmethod.definitions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class PixDefinitionTest {
    @Test
    fun `supports payment setup future use and setup intents`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.Pix),
        )

        assertThat(PixDefinition.isSupported(metadata)).isTrue()
        assertThat(PixDefinition.requiresMandate(metadata)).isEqualTo(
            intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
    }

    @Test
    fun `domestic form has no Pix specific fields`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("pix"),
                countryCode = "BR",
                currency = "brl",
            ),
        )

        assertThat(PixDefinition.formElements(metadata)).isEmpty()
    }

    @Test
    fun `international form collects required details and shows linked disclosure`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("pix"),
                countryCode = "US",
                currency = "brl",
            ),
        )

        val formElements = PixDefinition.formElements(metadata)

        assertThat(formElements.map { it.identifier.v1 }).containsExactly(
            "billing_details[name]_section",
            "billing_details[email]_section",
            "billing_details[tax_id]_section",
            "pix_international_disclosure",
        ).inOrder()
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThat(formElements.last().mandateText?.resolve(context)).isEqualTo(
            "This is an international purchase and excludes a 3.5% IOF fee. By proceeding, " +
                "you acknowledge and accept <a href=\"$EBANX_TERMS_URL\">Ebanx’s terms and conditions</a>."
        )
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(PixDefinition.supportedAsSavedPaymentMethod).isFalse()
    }

    private companion object {
        const val EBANX_TERMS_URL =
            "https://www.ebanx.com/pt-br/legal/consumidores/brasil/termos-para-processar-pagamentos/"
    }
}

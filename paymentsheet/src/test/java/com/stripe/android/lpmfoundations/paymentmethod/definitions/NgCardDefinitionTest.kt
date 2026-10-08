package com.stripe.android.lpmfoundations.paymentmethod.definitions

import android.content.Context
import android.text.style.URLSpan
import androidx.core.text.HtmlCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class NgCardDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.NgCard),
        )

        assertThat(NgCardDefinition.isSupported(metadata)).isEqualTo(
            intentScenario == LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
        assertThat(NgCardDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `billing address uses all supported countries`() {
        val formElements = NgCardDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_card")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
            initialValues = mapOf(FormFieldId.Country to "CA"),
        )

        val addressElement = (formElements.first() as SectionElement).fields.single() as AddressElement
        assertThat(addressElement.countryElement.controller.displayItems)
            .hasSize(CountryUtils.supportedBillingCountries.size)
        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `billing countries follow merchant configuration`(
        @TestParameter(value = ["Full", "AutomaticWithTax"])
        billingMode: LpmBillingDetailsCollectionMode,
    ) {
        val metadata = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.NgCard,
            billingDetailsCollectionMode = billingMode,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ).metadata().copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = billingMode.billingDetailsCollectionConfiguration().address,
                allowedCountries = setOf("CA"),
            ),
        )
        val formElements = NgCardDefinition.formElements(
            metadata = metadata,
            initialValues = mapOf(FormFieldId.Country to "CA"),
        )
        val addressField = formElements.filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .single()
        val countryElement = when (addressField) {
            is AddressElement -> addressField.countryElement
            is BillingAddressElement -> addressField.countryElement
            else -> error("Expected a billing address field")
        }

        assertThat(countryElement.controller.displayItems).containsExactly("🇨🇦 Canada")
        assertThat(countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `defaults billing country to Nigeria when no country is supplied`(
        @TestParameter(value = ["Full", "AutomaticWithTax"])
        billingMode: LpmBillingDetailsCollectionMode,
    ) {
        val elements = NgCardDefinition.formElements(
            metadata = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.NgCard,
                billingDetailsCollectionMode = billingMode,
                intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ).metadata(),
            initialValues = emptyMap(),
        )
        val field = elements.filterIsInstance<SectionElement>().single().fields.single()
        val country = when (field) {
            is AddressElement -> field.countryElement
            is BillingAddressElement -> field.countryElement
            else -> error("Expected billing address")
        }
        assertThat(country.controller.rawFieldValue.value).isEqualTo("NG")
        assertThat(country.controller.displayItems).hasSize(CountryUtils.supportedBillingCountries.size)
    }

    @Test
    fun `country default respects merchant restrictions`() {
        val elements = NgCardDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_card")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    allowedCountries = setOf("CA"),
                ),
            ),
            initialValues = emptyMap(),
        )
        val field = elements.filterIsInstance<SectionElement>().single().fields.single() as AddressElement
        assertThat(field.countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `rejects payment method specific future usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("ng_card"),
                paymentMethodOptionsJsonString = """{"ng_card":{"setup_future_usage":"off_session"}}""",
            ),
        )
        assertThat(NgCardDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `disclosure remains visible without collecting payment method fields`(
        @TestParameter termsDisplay: PaymentSheet.TermsDisplay,
    ) {
        val elements = NgCardDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_card")),
                termsDisplay = mapOf(PaymentMethod.Type.NgCard to termsDisplay),
            ),
        )
        assertThat(elements).hasSize(1)
        assertThat(elements.single()).isInstanceOf(MandateTextElement::class.java)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(NgCardDefinition.supportedAsSavedPaymentMethod).isFalse()
    }

    @Test
    fun `renders the merchant of record notice with a usable terms URL`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notice = context.getString(R.string.stripe_ng_payment_mor_notice, NIGERIAN_PAYMENT_METHOD_TERMS_URL)

        assertThat(HtmlCompat.fromHtml(notice, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()).isEqualTo(
            "By confirming your payment, you agree that your transaction will be handled by " +
                "Global Stack Services Limited as merchant of record and in accordance with their terms of use."
        )
        val renderedNotice = HtmlCompat.fromHtml(notice, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val links = renderedNotice.getSpans(0, renderedNotice.length, URLSpan::class.java)
        assertThat(links.map { it.url }).containsExactly(NIGERIAN_PAYMENT_METHOD_TERMS_URL)
        assertThat(notice).doesNotContain("%s")
    }
}

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
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import java.util.Locale

@RunWith(RobolectricTestParameterInjector::class)
internal class NgBankTransferDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.NgBankTransfer),
        )

        assertThat(NgBankTransferDefinition.isSupported(metadata)).isEqualTo(
            intentScenario == LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
        assertThat(NgBankTransferDefinition.requiresMandate(metadata)).isEqualTo(false)
    }

    @Test
    fun `billing address uses all supported countries`() {
        val formElements = NgBankTransferDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_bank_transfer")),
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
    fun `does not override the locale billing country with Nigeria`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val formElements = NgBankTransferDefinition.formElements(
                metadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_bank_transfer")),
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    ),
                ),
                initialValues = mapOf(FormFieldId.Country to null),
            )

            val addressElement = (formElements.first() as SectionElement).fields.single() as AddressElement
            assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("US")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `collects configured contact information before the disclosure`() {
        val formElements = NgBankTransferDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_bank_transfer")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
            )
        )

        assertThat(formElements).hasSize(4)
        checkNameField(formElements, 0)
        checkPhoneField(formElements, 1)
        checkEmailField(formElements, 2)
        val notice = formElements.last() as MandateTextElement
        assertThat(notice.stringResId).isEqualTo(R.string.stripe_ng_payment_mor_notice)
        assertThat(notice.args).containsExactly(NIGERIAN_PAYMENT_METHOD_TERMS_URL)
    }

    @Test
    fun `billing countries follow merchant configuration`(
        @TestParameter(value = ["Full", "AutomaticWithTax"])
        billingMode: LpmBillingDetailsCollectionMode,
    ) {
        val metadata = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.NgBankTransfer,
            billingDetailsCollectionMode = billingMode,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ).metadata().copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = billingMode.billingDetailsCollectionConfiguration().address,
                allowedCountries = setOf("CA"),
            ),
        )
        val formElements = NgBankTransferDefinition.formElements(
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
    fun `rejects payment method specific future usage`(
        @TestParameter(value = ["on_session", "off_session"]) futureUsage: String,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("ng_bank_transfer"),
                paymentMethodOptionsJsonString = """{"ng_bank_transfer":{"setup_future_usage":"$futureUsage"}}""",
            ),
        )
        assertThat(NgBankTransferDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `disclosure remains visible without collecting payment method fields`(
        @TestParameter termsDisplay: PaymentSheet.TermsDisplay,
    ) {
        val elements = NgBankTransferDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_bank_transfer")),
                termsDisplay = mapOf(PaymentMethod.Type.NgBankTransfer to termsDisplay),
            ),
        )
        assertThat(elements).hasSize(1)
        assertThat(elements.single()).isInstanceOf(MandateTextElement::class.java)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(NgBankTransferDefinition.supportedAsSavedPaymentMethod).isFalse()
    }

    @Test
    fun `renders the merchant of record notice with a usable terms URL`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notice = context.getString(R.string.stripe_ng_payment_mor_notice, NIGERIAN_PAYMENT_METHOD_TERMS_URL)

        assertThat(notice).contains("Global Stack Services Limited as merchant of record")
        val renderedNotice = HtmlCompat.fromHtml(notice, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val links = renderedNotice.getSpans(0, renderedNotice.length, URLSpan::class.java)
        assertThat(links.map { it.url }).containsExactly(NIGERIAN_PAYMENT_METHOD_TERMS_URL)
        assertThat(notice).doesNotContain("%s")
    }
}

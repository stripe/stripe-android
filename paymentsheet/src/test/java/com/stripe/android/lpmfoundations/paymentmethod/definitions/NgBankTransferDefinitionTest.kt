package com.stripe.android.lpmfoundations.paymentmethod.definitions

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
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.MandateTextElement
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
    fun `supplies the terms URL to the merchant of record disclosure`() {
        val formElements = NgBankTransferDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_bank_transfer")),
            )
        )

        val notice = formElements.single() as MandateTextElement
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
    fun `does not redisplay saved payment methods`() {
        assertThat(NgBankTransferDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}

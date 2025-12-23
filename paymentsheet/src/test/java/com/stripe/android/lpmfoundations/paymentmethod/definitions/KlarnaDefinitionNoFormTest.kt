package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class KlarnaDefinitionNoFormTest {
    @get:Rule
    val enableKlarnaFormRemovalRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableKlarnaFormRemoval,
        isEnabled = true
    )

    @BeforeTest
    fun enableKlarnaFormRemovalRule() {
        enableKlarnaFormRemovalRule.setEnabled(true)
    }

    @AfterTest
    fun disableKlarnaFormRemovalRule() {
        enableKlarnaFormRemovalRule.setEnabled(false)
    }

    @Test
    fun `createFormElements returns default set of fields when feature flag off`() {
        enableKlarnaFormRemovalRule.setEnabled(false)

        val formElements = KlarnaDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = getPaymentIntent(
                    setupFutureUsage = StripeIntent.Usage.OneTime
                ),
            )
        )
        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("klarna_header_text")
        assertThat(formElements[1].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("billing_details[address][country]_section")
    }

    @Test
    fun `createFormElements returns default set of fields for payment intent`() {
        val formElements = KlarnaDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = getPaymentIntent(
                    setupFutureUsage = StripeIntent.Usage.OneTime
                ),
            )
        )
        assertThat(formElements).hasSize(0)
    }

    @Test
    fun `createFormElements returns default set of fields for payment intent with setup`() {
        val formElements = KlarnaDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = getPaymentIntent(
                    setupFutureUsage = StripeIntent.Usage.OffSession
                ),
                termsDisplay = mapOf(
                    PaymentMethod.Type.Klarna to PaymentSheet.TermsDisplay.AUTOMATIC
                )
            )
        )
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("mandate")
    }

    @Test
    fun `createFormElements returns default set of fields for setup intent`() {
        val formElements = KlarnaDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = getSetupIntent(),
                termsDisplay = mapOf(
                    PaymentMethod.Type.Klarna to PaymentSheet.TermsDisplay.AUTOMATIC
                )
            )
        )
        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1)
            .isEqualTo("klarna_header_text")
        assertThat(formElements[1].identifier.v1)
            .isEqualTo("billing_details[address][country]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("mandate")
    }

    @Test
    fun `createFormElements requested billing details fields for payment intent`() {
        val formElements = KlarnaDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = getPaymentIntent(
                    setupFutureUsage = StripeIntent.Usage.OneTime
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(2)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[1].identifier.v1)
            .isEqualTo("billing_details[address]_section")

        assertThatFullAddressElementIsPresent(formElements[1] as SectionElement)
    }

    @Test
    fun `createFormElements requested billing details fields for setup intent`() {
        val formElements = KlarnaDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = getSetupIntent(),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[1].identifier.v1)
            .isEqualTo("billing_details[address]_section")

        assertThatFullAddressElementIsPresent(formElements[1] as SectionElement)

        assertThat(formElements[2].identifier.v1)
            .isEqualTo("mandate")
    }

    private fun assertThatFullAddressElementIsPresent(
        sectionElement: SectionElement
    ) {
        val addressElement = (sectionElement).fields[0] as AddressElement

        val billingElements = addressElement.addressController.value.fieldsFlowable.value
        assertThat(billingElements.size).isEqualTo(5)

        assertThat(billingElements[0].identifier.v1).isEqualTo("billing_details[address][country]")
        assertThat(billingElements[1].identifier.v1).isEqualTo("billing_details[address][line1]")
        assertThat(billingElements[2].identifier.v1).isEqualTo("billing_details[address][line2]")

        val rowElement = billingElements[3] as RowElement

        assertThat(rowElement.fields[0].identifier.v1).isEqualTo("billing_details[address][city]")
        assertThat(rowElement.fields[1].identifier.v1).isEqualTo("billing_details[address][postal_code]")

        assertThat(billingElements[4].identifier.v1).isEqualTo("billing_details[address][state]")
    }

    private fun getPaymentIntent(
        setupFutureUsage: StripeIntent.Usage
    ): PaymentIntent {
        return PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "klarna"),
            setupFutureUsage = setupFutureUsage,
        )
    }

    private fun getSetupIntent(): SetupIntent {
        return SetupIntentFactory.create(
            paymentMethodTypes = listOf("card", "klarna"),
        )
    }
}

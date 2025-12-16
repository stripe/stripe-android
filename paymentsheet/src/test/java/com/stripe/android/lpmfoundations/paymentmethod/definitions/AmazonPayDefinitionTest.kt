package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmazonPayDefinitionTest {
    @Test
    fun `createFormElements returns no elements if payment intent`() {
        val formElements = AmazonPayDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("amazon_pay")
                ),
            )
        )

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `createFormElements returns mandate if setup intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("amazon_pay")
            ),
        )

        val formElements = AmazonPayDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("mandate")
        assertThat(formElements[0]).isInstanceOf<MandateTextElement>()

        val mandateElement = formElements[0] as MandateTextElement

        assertThat(mandateElement.stringResId)
            .isEqualTo(R.string.stripe_amazon_pay_mandate)
        assertThat(mandateElement.args)
            .isEqualTo(listOf(metadata.merchantName))
    }

    @Test
    fun `createFormElements returns no elements if terms display is set to never`() {
        val formElements = AmazonPayDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFactory.create(
                    paymentMethodTypes = listOf("amazon_pay")
                ),
                termsDisplay = mapOf(
                    PaymentMethod.Type.AmazonPay to PaymentSheet.TermsDisplay.NEVER,
                )
            )
        )

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `createFormElements returns mandate & requested contact information fields`() {
        val formElements = AmazonPayDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFactory.create(
                    paymentMethodTypes = listOf("amazon_pay")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(3)

        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)

        assertThat(formElements[2]).isInstanceOf<MandateTextElement>()
    }

    @Test
    fun `createFormElements returns mandate & all billing details fields`() {
        val formElements = AmazonPayDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFactory.create(
                    paymentMethodTypes = listOf("amazon_pay")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(5)

        checkNameField(formElements, 0)
        checkPhoneField(formElements, 1)
        checkEmailField(formElements, 2)
        checkBillingField(formElements, 3)

        assertThat(formElements[4]).isInstanceOf<MandateTextElement>()
    }
}

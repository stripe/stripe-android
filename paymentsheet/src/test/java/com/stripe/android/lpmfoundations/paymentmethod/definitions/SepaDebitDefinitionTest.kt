package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import org.junit.Test

class SepaDebitDefinitionTest {
    @Test
    fun `'createFormElements' includes 'SaveForFutureUseElement' when changeable`() {
        val formElements = SepaDebitDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("sepa_debit")
                ),
                hasCustomerConfiguration = true
            )
        )

        val saveForFutureUseElement = formElements.find { it is SaveForFutureUseElement }
        assertThat(saveForFutureUseElement).isNotNull()
        assertThat(saveForFutureUseElement).isInstanceOf(SaveForFutureUseElement::class.java)
    }

    @Test
    fun `'createFormElements' includes 'SetAsDefaultPaymentMethodElement' when enabled`() {
        val formElements = SepaDebitDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("sepa_debit")
                ),
                hasCustomerConfiguration = true,
                isPaymentMethodSetAsDefaultEnabled = true
            )
        )

        val setAsDefaultElement = formElements.find { it is SetAsDefaultPaymentMethodElement }
        assertThat(setAsDefaultElement).isNotNull()
        assertThat(setAsDefaultElement).isInstanceOf(SetAsDefaultPaymentMethodElement::class.java)
    }

    @Test
    fun `'createFormElements' does not include 'SetAsDefaultPaymentMethodElement' when disabled`() {
        val formElements = SepaDebitDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("sepa_debit")
                ),
                hasCustomerConfiguration = true,
                isPaymentMethodSetAsDefaultEnabled = false
            )
        )

        val setAsDefaultElement = formElements.find { it is SetAsDefaultPaymentMethodElement }
        assertThat(setAsDefaultElement).isNull()
    }

    @Test
    fun `'createFormElements' with SetupIntent includes 'SaveForFutureUseElement' when save behavior is enabled`() {
        val formElements = SepaDebitDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("sepa_debit")
                ),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                hasCustomerConfiguration = true
            )
        )

        val saveForFutureUseElement = formElements.find { it is SaveForFutureUseElement }
        assertThat(saveForFutureUseElement).isNotNull()
    }

    @Test
    fun `'createFormElements' always includes mandate as last element`() {
        val formElements = SepaDebitDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("sepa_debit")
                ),
                hasCustomerConfiguration = true,
                isPaymentMethodSetAsDefaultEnabled = true
            )
        )

        val setAsDefaultElement = formElements.find { it is SetAsDefaultPaymentMethodElement }
        assertThat(setAsDefaultElement).isNotNull()

        val lastElement = formElements.last()

        assertThat(setAsDefaultElement).isNotEqualTo(lastElement)

        assertThat(lastElement).isInstanceOf(MandateTextElement::class.java)
    }
}

package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KakaoPayDefinitionTest {
    @Test
    fun `createFormElements returns email field for payment intent`() {
        val formElements = KakaoPayDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("kakao_pay"),
                ),
            )
        )

        assertThat(formElements).hasSize(1)
        checkEmailField(formElements, 0)
    }

    @Test
    fun `createFormElements returns email field and mandate for setup intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("kakao_pay"),
            ),
        )

        val formElements = KakaoPayDefinition.formElements(metadata)

        assertThat(formElements).hasSize(2)
        checkEmailField(formElements, 0)
        val mandateElement = formElements[1]
        assertThat(mandateElement).isInstanceOf<MandateTextElement>()
        val mandate = mandateElement as MandateTextElement
        assertThat(mandate.stringResId).isEqualTo(R.string.stripe_kr_card_mandate)
        assertThat(mandate.args).containsExactly(metadata.merchantName)
    }

    @Test
    fun `createFormElements returns mandate for payment intent with future usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("kakao_pay"),
            ).copy(
                setupFutureUsage = StripeIntent.Usage.OffSession,
            ),
        )

        val formElements = KakaoPayDefinition.formElements(metadata)

        assertThat(formElements).hasSize(2)
        checkEmailField(formElements, 0)
        assertThat(formElements[1]).isInstanceOf<MandateTextElement>()
    }

    @Test
    fun `createFormElements omits mandate if terms display is never`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("kakao_pay"),
            ),
            termsDisplay = mapOf(
                PaymentMethod.Type.KakaoPay to PaymentSheet.TermsDisplay.NEVER,
            ),
        )

        val formElements = KakaoPayDefinition.formElements(metadata)

        assertThat(formElements).hasSize(1)
        checkEmailField(formElements, 0)
    }

    @Test
    fun `requirements allow setup intents`() {
        assertThat(KakaoPayDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = true)).isEmpty()
    }

    @Test
    fun `saved payment methods are not supported`() {
        assertThat(KakaoPayDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}

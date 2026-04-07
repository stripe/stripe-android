package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.elements.PaymentMethodMessageHeaderElement
import com.stripe.android.utils.FakePaymentMethodMessagingPromotionsHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AfterpayClearpayDefinitionTest {

    @Test
    fun `createFormElements includes promotion if available`() {
        val formElements = AfterpayClearpayDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("afterpay_clearpay")
                ),
            ),
            paymentMethodMessagingPromotionsHelper = FakePaymentMethodMessagingPromotionsHelper(
                promotions = listOf(
                    PaymentMethodMessagePromotion(
                        paymentMethodType = "Afterpay_Clearpay",
                        message = "This is a promotion",
                        learnMore = PaymentMethodMessageLearnMore(
                            url = "https://test.com",
                            message = "Click me."
                        )
                    )
                )
            )
        )

        assertThat(formElements).hasSize(3)

        val element = formElements[0]
        assertThat(element.identifier.v1).isEqualTo("afterpay_promotion")
        assertThat(element).isInstanceOf< PaymentMethodMessageHeaderElement>()
        val headerElement = element as PaymentMethodMessageHeaderElement
        assertThat(headerElement.messagePromotion.message).isEqualTo("This is a promotion")
        assertThat(headerElement.messagePromotion.paymentMethodType).isEqualTo("Afterpay_Clearpay")
        assertThat(headerElement.messagePromotion.learnMore).isEqualTo(
            PaymentMethodMessageLearnMore(
                url = "https://test.com",
                message = "Click me."
            )
        )
    }
}
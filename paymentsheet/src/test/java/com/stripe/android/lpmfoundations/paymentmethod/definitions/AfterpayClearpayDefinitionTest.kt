package com.stripe.android.lpmfoundations.paymentmethod.definitions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.PaymentMethodMessageHeaderElement
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.paymentsheet.R as PaymentSheetR

@RunWith(RobolectricTestRunner::class)
class AfterpayClearpayDefinitionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val res = context.resources

    @Test
    fun `Test label for afterpay show correctly when clearpay string`() {
        testDisplayNameReflectsLocale(
            currency = "GBP",
            titleResourceId = R.string.stripe_paymentsheet_payment_method_clearpay,
            subtitleResourceId = PaymentSheetR.string.stripe_clearpay_subtitle,
        )
    }

    @Test
    fun `Test label for afterpay show correctly when afterpay string`() {
        testDisplayNameReflectsLocale(
            currency = "EUR",
            titleResourceId = R.string.stripe_paymentsheet_payment_method_afterpay,
            subtitleResourceId = PaymentSheetR.string.stripe_afterpay_subtitle,
        )
    }

    @Test
    fun `Test label for afterpay show correctly when cashapp afterpay string`() {
        testDisplayNameReflectsLocale(
            currency = "USD",
            titleResourceId = R.string.stripe_paymentsheet_payment_method_afterpay,
            subtitleResourceId = PaymentSheetR.string.stripe_cashapp_afterpay_subtitle,
        )
    }

    @Test
    fun `createFormElements includes promotion if available`() {
        val formElements = AfterpayClearpayDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("afterpay_clearpay")
                ),
            ),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(
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

        assertThat(formElements).hasSize(2)

        val element = formElements[0]
        assertThat(element.identifier.v1).isEqualTo("afterpay_promotion")
        assertThat(element).isInstanceOf<PaymentMethodMessageHeaderElement>()
        val headerElement = element as PaymentMethodMessageHeaderElement
        assertThat(headerElement.promotion.message).isEqualTo("This is a promotion")
        assertThat(headerElement.promotion.paymentMethodType).isEqualTo("Afterpay_Clearpay")
        assertThat(headerElement.promotion.learnMore).isEqualTo(
            PaymentMethodMessageLearnMore(
                url = "https://test.com",
                message = "Click me."
            )
        )
    }

    private fun testDisplayNameReflectsLocale(currency: String, titleResourceId: Int, subtitleResourceId: Int) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("card", "afterpay_clearpay"),
                currency = currency,
            )
        )
        val supportedPaymentMethod = paymentMethodMetadata.supportedPaymentMethodForCode(
            code = "afterpay_clearpay",
        )!!
        assertThat(supportedPaymentMethod.displayName.resolve(context))
            .isEqualTo(res.getString(titleResourceId))
        assertThat(supportedPaymentMethod.subtitle?.resolve(context))
            .isEqualTo(res.getString(subtitleResourceId))
    }
}

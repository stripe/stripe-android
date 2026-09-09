package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NaverPayDefinitionTest {
    @Test
    fun `createFormElements returns funding dropdown`() {
        val formElements = NaverPayDefinition.formElements(paymentMetadata())

        assertThat(formElements).hasSize(1)
        val fundingSection = formElements.single() as SectionElement
        val fundingElement = fundingSection.fields.single() as SimpleDropdownElement
        val controller = fundingElement.controller as DropdownFieldController

        assertThat(fundingElement.identifier.v1).isEqualTo("naver_pay[funding]")
        assertThat(controller.displayItems).containsExactly(
            "Naver Pay Card",
            "Naver Pay Money/Point",
        ).inOrder()
        assertThat(controller.rawFieldValue.value).isEqualTo("card")
    }

    @Test
    fun `createFormElements restores selected funding`() {
        val formElements = NaverPayDefinition.formElements(
            metadata = paymentMetadata(),
            paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
                code = "naver_pay",
                billingDetails = null,
                requiresMandate = false,
                overrideParamMap = mapOf(
                    "type" to "naver_pay",
                    "naver_pay" to mapOf("funding" to "points"),
                ),
                productUsage = emptySet(),
                clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
            ),
        )

        val fundingSection = formElements.single() as SectionElement
        val fundingElement = fundingSection.fields.single() as SimpleDropdownElement

        assertThat(fundingElement.controller.rawFieldValue.value).isEqualTo("points")
    }

    @Test
    fun `createFormElements returns funding dropdown and mandate for setup intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("naver_pay"),
            ),
        )

        val formElements = NaverPayDefinition.formElements(metadata)

        assertThat(formElements).hasSize(2)
        assertThat(formElements[0]).isInstanceOf<SectionElement>()
        val mandate = formElements[1] as MandateTextElement
        assertThat(mandate.stringResId).isEqualTo(R.string.stripe_kr_card_mandate)
        assertThat(mandate.args).containsExactly(metadata.merchantName)
    }

    @Test
    fun `createFormElements omits mandate if terms display is never`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("naver_pay"),
            ),
            termsDisplay = mapOf(
                PaymentMethod.Type.NaverPay to PaymentSheet.TermsDisplay.NEVER,
            ),
        )

        val formElements = NaverPayDefinition.formElements(metadata)

        assertThat(formElements).hasSize(1)
        assertThat(formElements.single()).isInstanceOf<SectionElement>()
    }

    private fun paymentMetadata() = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("naver_pay"),
        ),
    )
}

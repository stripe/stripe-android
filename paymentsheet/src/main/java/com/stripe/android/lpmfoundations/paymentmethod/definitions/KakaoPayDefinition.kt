package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement

internal object KakaoPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.KakaoPay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = KakaoPayUiDefinitionFactory
}

private object KakaoPayUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = KakaoPayDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_kakao_pay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_kakao_pay,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_kakao_pay_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder.requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)

        if (KakaoPayDefinition.requiresMandate(metadata)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_kr_card_mandate,
                    args = listOf(arguments.merchantName),
                )
            )
        }
    }
}

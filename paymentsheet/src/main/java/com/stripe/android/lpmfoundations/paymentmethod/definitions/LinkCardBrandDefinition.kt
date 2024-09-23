package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkBankPaymentUiDefinitionFactory
import com.stripe.android.model.PaymentMethod

internal object LinkCardBrandDefinition : PaymentMethodDefinition {

    override val type: PaymentMethod.Type = PaymentMethod.Type.Link

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.LinkCardBrand,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = LinkBankPaymentUiDefinitionFactory
}

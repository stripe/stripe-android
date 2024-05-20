package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.ui.core.R as PaymentsUiCoreR

internal object InstantDebitsDefinition : PaymentMethodDefinition {

    override val type: PaymentMethod.Type = PaymentMethod.Type.Link

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.InstantDebits,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = InstantDebitsUiDefinitionFactory
}

private object InstantDebitsUiDefinitionFactory : UiDefinitionFactory.Simple {

    override fun createSupportedPaymentMethod(): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = InstantDebitsDefinition.type.code,
            displayNameResource = PaymentsUiCoreR.string.stripe_paymentsheet_payment_method_instant_debits,
            iconResource = PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_bank,
            iconRequiresTinting = true,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )
    }

    // Instant Debits uses its own mechanism, not these form elements.
    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        return emptyList()
    }
}

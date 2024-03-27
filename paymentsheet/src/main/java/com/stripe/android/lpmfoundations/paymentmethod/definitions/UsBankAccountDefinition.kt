package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormElement

internal object UsBankAccountDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.USBankAccount

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.ValidUsBankVerificationMethod,
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = UsBankAccountUiDefinitionFactory
}

private object UsBankAccountUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(): SupportedPaymentMethod = SupportedPaymentMethod(
        code = UsBankAccountDefinition.type.code,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
        tintIconOnSelection = true,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
    )

    // US Bank Account uses it's own mechanism, not these form elements.
    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> = emptyList()
}

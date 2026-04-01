package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.ui.core.R as UiCoreR

internal object AfterpayClearpayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.AfterpayClearpay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = AfterpayClearpayUiDefinitionFactory
}

private object AfterpayClearpayUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = AfterpayClearpayDefinition,
        displayNameResource = if (AfterpayClearpayHeaderElement.isClearpay(metadata.stripeIntent.currency)) {
            UiCoreR.string.stripe_paymentsheet_payment_method_clearpay
        } else {
            UiCoreR.string.stripe_paymentsheet_payment_method_afterpay
        },
        iconResource = if (AfterpayClearpayHeaderElement.isCashappAfterpay(metadata.stripeIntent.currency)) {
            UiCoreR.drawable.stripe_ic_paymentsheet_pm_cash_app_pay
        } else {
            UiCoreR.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay
        },
        iconResourceNight = null,
        subtitle = if (AfterpayClearpayHeaderElement.isClearpay(metadata.stripeIntent.currency)) {
            R.string.stripe_clearpay_subtitle.resolvableString
        } else if (AfterpayClearpayHeaderElement.isCashappAfterpay(metadata.stripeIntent.currency)) {
            R.string.stripe_cashapp_afterpay_subtitle.resolvableString
        } else {
            R.string.stripe_afterpay_subtitle.resolvableString
        },
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder
            .header(
                AfterpayClearpayHeaderElement(
                    identifier = IdentifierSpec.Generic("afterpay_header"),
                    currency = metadata.stripeIntent.currency
                )
            )
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
    }
}

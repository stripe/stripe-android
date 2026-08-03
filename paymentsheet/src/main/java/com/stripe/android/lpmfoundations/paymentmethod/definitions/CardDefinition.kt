package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.CardFormElementRenderer
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.ui.core.R as PaymentsUiCoreR

internal object CardDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Card

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory {
        return CardUiDefinitionFactory
    }
}

private object CardUiDefinitionFactory : UiDefinitionFactory.Custom {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
    ): SupportedPaymentMethod {
        val iconResource = if (metadata.isTapToAddSupported) {
            PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_card_with_tap
        } else {
            PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_card
        }

        val outlinedIconResource = if (metadata.isTapToAddSupported) {
            PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_card_with_tap
        } else {
            PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_card_outlined
        }

        val subtitle = if (metadata.isTapToAddSupported) {
            PaymentsUiCoreR.string.stripe_card_with_tap_or_enter_manually
        } else {
            null
        }

        return SupportedPaymentMethod(
            paymentMethodDefinition = CardDefinition,
            displayNameResource = PaymentsUiCoreR.string.stripe_paymentsheet_payment_method_card,
            iconResource = iconResource,
            iconResourceNight = null,
            subtitle = subtitle?.resolvableString,
            outlinedIconResource = outlinedIconResource,
            iconRequiresTinting = true,
        )
    }

    override fun createFormHeaderInformation(
        metadata: PaymentMethodMetadata,
        customerHasSavedPaymentMethods: Boolean,
        incentive: PaymentMethodIncentive?,
    ): FormHeaderInformation {
        val displayName = if (customerHasSavedPaymentMethods) {
            PaymentsUiCoreR.string.stripe_paymentsheet_add_new_card
        } else {
            PaymentsUiCoreR.string.stripe_paymentsheet_add_card
        }
        return createSupportedPaymentMethod(metadata).asFormHeaderInformation(incentive).copy(
            displayName = displayName.resolvableString,
            shouldShowIcon = false,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        return CardFormElementRenderer.details(metadata, arguments) +
            CardFormElementRenderer.billingDetails(metadata, arguments) +
            CardFormElementRenderer.savePaymentMethod(metadata, arguments) +
            CardFormElementRenderer.linkInlineSignup(metadata, arguments) +
            CardFormElementRenderer.mandate(metadata, arguments)
    }
}

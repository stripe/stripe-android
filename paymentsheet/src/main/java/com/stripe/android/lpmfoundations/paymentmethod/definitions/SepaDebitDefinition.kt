package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.luxe.addSavePaymentOptionElements
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement

internal object SepaDebitDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.SepaDebit

    override val supportedAsSavedPaymentMethod: Boolean = true

    override val supportsTermDisplayConfiguration: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = SepaDebitUiDefinitionFactory
}

private object SepaDebitUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
        transformSpecToElements: TransformSpecToElements,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val canSave = isSaveForFutureUseValueChangeable(
            code = SepaDebitDefinition.type.code,
            metadata = metadata,
        )

        val sharedSpecElements = super.createFormElements(metadata, sharedDataSpec, transformSpecToElements)

        return if (canSave) {
            sharedSpecElements.toMutableList()
                .apply {
                    val lastElement = lastOrNull()

                    val mandate = if (lastElement is MandateTextElement) {
                        removeAt(lastIndex)
                    } else {
                        null
                    }

                    addSavePaymentOptionElements(
                        metadata = metadata,
                        arguments = arguments,
                    )

                    mandate?.let { add(it) }
                }
                .toList()
        } else {
            sharedSpecElements
        }
    }

    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = SepaDebitDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_sepa_debit,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_sepa_debit,
        iconResourceNight = null,
    )
}

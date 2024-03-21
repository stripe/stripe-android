package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement

internal sealed interface UiDefinitionFactory {
    interface RequiresSharedDataSpec : UiDefinitionFactory {
        fun createSupportedPaymentMethod(
            sharedDataSpec: SharedDataSpec,
        ): SupportedPaymentMethod

        fun createFormElements(
            metadata: PaymentMethodMetadata,
            sharedDataSpec: SharedDataSpec,
            transformSpecToElements: TransformSpecToElements,
        ): List<FormElement> {
            return transformSpecToElements.transform(
                specs = sharedDataSpec.fields,
            )
        }
    }

    interface Simple : UiDefinitionFactory {
        fun createSupportedPaymentMethod(): SupportedPaymentMethod
        fun createFormElements(metadata: PaymentMethodMetadata): List<FormElement>
    }

    fun canBeDisplayedInUi(
        definition: PaymentMethodDefinition,
        sharedDataSpecs: List<SharedDataSpec>,
    ): Boolean = when (this) {
        is Simple -> {
            true
        }

        is RequiresSharedDataSpec -> {
            sharedDataSpecs.firstOrNull { it.type == definition.type.code } != null
        }
    }

    fun supportedPaymentMethod(
        definition: PaymentMethodDefinition,
        sharedDataSpecs: List<SharedDataSpec>,
    ): SupportedPaymentMethod? = when (this) {
        is Simple -> {
            createSupportedPaymentMethod()
        }

        is RequiresSharedDataSpec -> {
            val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == definition.type.code }
            if (sharedDataSpec != null) {
                createSupportedPaymentMethod(sharedDataSpec)
            } else {
                null
            }
        }
    }

    fun formElements(
        definition: PaymentMethodDefinition,
        metadata: PaymentMethodMetadata,
        sharedDataSpecs: List<SharedDataSpec>,
        transformSpecToElements: TransformSpecToElements,
    ): List<FormElement>? = when (this) {
        is Simple -> {
            createFormElements(metadata)
        }

        is RequiresSharedDataSpec -> {
            val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == definition.type.code }
            if (sharedDataSpec != null) {
                createFormElements(metadata, sharedDataSpec, transformSpecToElements)
            } else {
                null
            }
        }
    }
}

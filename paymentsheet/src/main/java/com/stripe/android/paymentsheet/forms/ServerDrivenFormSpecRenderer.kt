package com.stripe.android.paymentsheet.forms

import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.paymentsheet.forms.generated.FormElementSpecV1 as FormElementSpec
import com.stripe.android.paymentsheet.forms.generated.PaymentMethodFormSpecV1 as PaymentMethodFormSpec

@Suppress("TooGenericExceptionCaught")
internal fun PaymentMethodFormSpec.createFormElements(
    metadata: PaymentMethodMetadata,
    sharedDataSpecs: List<SharedDataSpec>,
    argumentsFactory: UiDefinitionFactory.Arguments.Factory,
): List<FormElement> {
    try {
        return createFormElementsOrThrow(
            metadata = metadata,
            sharedDataSpecs = sharedDataSpecs,
            argumentsFactory = argumentsFactory,
        )
    } catch (error: ServerDrivenFormRenderException) {
        throw error
    } catch (error: Throwable) {
        throw ServerDrivenFormRenderException(
            errorCode = ServerDrivenFormRenderException.ErrorCode.FormRenderFailure,
            cause = error,
        )
    }
}

private fun PaymentMethodFormSpec.createFormElementsOrThrow(
    metadata: PaymentMethodMetadata,
    sharedDataSpecs: List<SharedDataSpec>,
    argumentsFactory: UiDefinitionFactory.Arguments.Factory,
): List<FormElement> {
    val declarativeFieldCount = fields.count { it.isDeclarative() }
    val declarativeSpecs = if (declarativeFieldCount == 0) {
        emptyList()
    } else {
        val sharedDataSpec = requireNotNull(sharedDataSpecs.firstOrNull { it.type == type }) {
            "Missing rendered server-driven form spec for payment method '$type'"
        }
        require(sharedDataSpec.fields.size == declarativeFieldCount) {
            "Rendered field count did not match server-driven form spec for payment method '$type'"
        }
        sharedDataSpec.fields
    }
    val arguments = argumentsFactory.create(
        metadata = metadata,
        requiresMandate = fields.any { it.isMandate() },
    )
    val declarativeIterator = declarativeSpecs.iterator()
    return fields.flatMap { field ->
        when (field.type) {
            "native_component" -> {
                val component = requireNotNull(field.component) {
                    "Missing component for native component '$type'"
                }
                NativeComponentRenderer.render(
                    component = component,
                    paymentMethodCode = type,
                    metadata = metadata,
                    argumentsFactory = argumentsFactory,
                )
            }
            "mandate_text" -> listOfNotNull(
                MandateTextRenderer.render(
                    spec = field,
                    metadata = metadata,
                    paymentMethodType = type,
                    arguments = arguments,
                )
            )
            else -> TransformSpecToElements(arguments).transform(
                metadata = metadata,
                specs = listOf(declarativeIterator.next()),
                termsDisplay = metadata.termsDisplayForCode(type),
                applyBillingDetailsConfiguration = false,
            )
        }
    }
}

private fun FormElementSpec.isDeclarative(): Boolean {
    return type != "native_component" && type != "mandate_text"
}

private fun FormElementSpec.isMandate(): Boolean {
    return type == "mandate_text" || type == "sepa_mandate" || type == "au_becs_mandate"
}

package com.stripe.android.lpmfoundations

import androidx.compose.ui.unit.Dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId

/** Describes a form without creating controllers or reading the current selection. */
internal class FormDefinition(val elements: List<FormElementDefinition>) {
    val allowsUserInteraction: Boolean
        get() = elements.any { it.allowsUserInteraction }

    val mandateText: ResolvableString?
        get() = elements.firstNotNullOfOrNull { it.mandateText }

    fun createFormElements(arguments: UiDefinitionFactory.Arguments): List<FormElement> {
        return elements.flatMap { it.createFormElements(arguments) }
    }
}

/** A factory may create related elements together so they share fresh controller state. */
internal class FormElementDefinition(
    val allowsUserInteraction: Boolean,
    val mandateText: ResolvableString?,
    val createFormElements: (UiDefinitionFactory.Arguments) -> List<FormElement>,
) {
    companion object {
        fun interactive(factory: (UiDefinitionFactory.Arguments) -> FormElement): FormElementDefinition {
            return FormElementDefinition(true, null) { listOf(factory(it)) }
        }

        fun nonInteractive(factory: (UiDefinitionFactory.Arguments) -> FormElement): FormElementDefinition {
            return FormElementDefinition(false, null) { listOf(factory(it)) }
        }

        fun mandate(
            stringResId: Int,
            args: List<String>,
            identifier: FormFieldId,
            topPadding: Dp,
        ): FormElementDefinition {
            val mandate = MandateDefinition(identifier, stringResId, args)
            return FormElementDefinition(false, mandate.text) { listOf(mandate.createElement(topPadding)) }
        }
    }
}

/** Shares the mandate's text configuration between inspection and rendering. */
internal class MandateDefinition(
    private val identifier: FormFieldId,
    private val stringResId: Int,
    private val args: List<String>,
) {
    val text: ResolvableString = resolvableString(stringResId, *args.toTypedArray())

    fun createElement(topPadding: Dp): MandateTextElement {
        return MandateTextElement(identifier, stringResId, args, topPadding)
    }
}

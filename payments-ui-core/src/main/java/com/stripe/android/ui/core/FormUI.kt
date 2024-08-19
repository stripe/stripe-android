package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.ui.core.elements.AffirmElementUI
import com.stripe.android.ui.core.elements.AffirmHeaderElement
import com.stripe.android.ui.core.elements.AfterpayClearpayElementUI
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement
import com.stripe.android.ui.core.elements.AuBecsDebitMandateElementUI
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextElement
import com.stripe.android.ui.core.elements.BsbElement
import com.stripe.android.ui.core.elements.BsbElementUI
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.CardDetailsSectionElementUI
import com.stripe.android.ui.core.elements.EmptyFormElement
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.MandateTextUI
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.ui.core.elements.StaticTextElementUI
import com.stripe.android.uicore.elements.CheckboxFieldElement
import com.stripe.android.uicore.elements.CheckboxFieldUI
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.StateFlow

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FormUI(
    hiddenIdentifiersFlow: StateFlow<Set<IdentifierSpec>>,
    enabledFlow: StateFlow<Boolean>,
    elementsFlow: StateFlow<List<FormElement>>,
    lastTextFieldIdentifierFlow: StateFlow<IdentifierSpec?>,
    modifier: Modifier = Modifier
) {
    val hiddenIdentifiers by hiddenIdentifiersFlow.collectAsState()
    val enabled by enabledFlow.collectAsState()
    val elements by elementsFlow.collectAsState()
    val lastTextFieldIdentifier by lastTextFieldIdentifierFlow.collectAsState()

    FormUI(
        hiddenIdentifiers = hiddenIdentifiers,
        enabled = enabled,
        elements = elements,
        lastTextFieldIdentifier = lastTextFieldIdentifier,
        modifier = modifier
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FormUI(
    hiddenIdentifiers: Set<IdentifierSpec>,
    enabled: Boolean,
    elements: List<FormElement>,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(1f)
    ) {
        elements.forEachIndexed { _, element ->
            if (!hiddenIdentifiers.contains(element.identifier)) {
                FormUIElement(
                    element = element,
                    enabled = enabled,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    hiddenIdentifiers = hiddenIdentifiers,
                )
            }
        }
    }
}

@Composable
private fun FormUIElement(
    element: FormElement,
    enabled: Boolean,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?,
) {
    when (element) {
        is SectionElement -> SectionElementUI(
            enabled,
            element,
            hiddenIdentifiers,
            lastTextFieldIdentifier
        )
        is CheckboxFieldElement -> CheckboxFieldUI(
            controller = element.controller,
            enabled = enabled
        )
        is StaticTextElement -> StaticTextElementUI(element)
        is SaveForFutureUseElement -> SaveForFutureUseElementUI(enabled, element)
        is AfterpayClearpayHeaderElement -> AfterpayClearpayElementUI(
            enabled,
            element
        )
        is AuBecsDebitMandateTextElement -> AuBecsDebitMandateElementUI(element)
        is AffirmHeaderElement -> AffirmElementUI()
        is MandateTextElement -> MandateTextUI(element)
        is CardDetailsSectionElement -> CardDetailsSectionElementUI(
            enabled,
            element.controller,
            hiddenIdentifiers,
            lastTextFieldIdentifier
        )
        is BsbElement -> BsbElementUI(enabled, element, lastTextFieldIdentifier)
        is OTPElement -> OTPElementUI(enabled, element)
        is RenderableFormElement -> element.ComposeUI(enabled)
        is EmptyFormElement -> {}
    }
}

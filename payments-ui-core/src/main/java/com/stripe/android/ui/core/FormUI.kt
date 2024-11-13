package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SameAsShippingElementUI
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
        val visibleElements = elements.filter { element ->
            !hiddenIdentifiers.contains(element.identifier) && element !is EmptyFormElement
        }

        visibleElements.forEachIndexed { index, element ->
            FormUIElement(
                element = element,
                enabled = enabled,
                index = index,
                maxIndex = visibleElements.size - 1,
                lastTextFieldIdentifier = lastTextFieldIdentifier,
                hiddenIdentifiers = hiddenIdentifiers,
            )
        }
    }
}

@Composable
private fun FormUIElement(
    element: FormElement,
    index: Int,
    maxIndex: Int,
    enabled: Boolean,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?,
) {
    when (element) {
        is SectionElement -> SectionElementUI(
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 8.dp,
            ),
            enabled = enabled,
            element = element,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
        )
        is CheckboxFieldElement -> CheckboxFieldUI(
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 4.dp,
            ),
            controller = element.controller,
            enabled = enabled
        )
        is StaticTextElement -> StaticTextElementUI(
            element = element,
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 8.dp,
            ),
        )
        is SaveForFutureUseElement -> SaveForFutureUseElementUI(
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 4.dp,
            ),
            enabled = enabled,
            element = element,
        )
        is SameAsShippingElement -> SameAsShippingElementUI(
            controller = element.controller,
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 4.dp,
            ),
        )
        is AfterpayClearpayHeaderElement -> AfterpayClearpayElementUI(
            enabled = enabled,
            element = element,
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                start = 4.dp,
                end = 4.dp,
                top = 8.dp,
                bottom = 4.dp,
            ),
        )
        is AuBecsDebitMandateTextElement -> AuBecsDebitMandateElementUI(
            element = element,
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 8.dp,
            ),
        )
        is AffirmHeaderElement -> AffirmElementUI(
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 8.dp,
            ),
        )
        is MandateTextElement -> MandateTextUI(
            element = element,
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                top = element.topPadding,
                bottom = 8.dp,
            ),
        )
        is CardDetailsSectionElement -> CardDetailsSectionElementUI(
            enabled = enabled,
            controller = element.controller,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 8.dp,
            ),
        )
        is BsbElement -> BsbElementUI(
            enabled = enabled,
            element = element,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            modifier = Modifier.formVerticalPadding(
                maxIndex = maxIndex,
                index = index,
                vertical = 8.dp,
            ),
        )
        is OTPElement -> OTPElementUI(enabled, element)
        is RenderableFormElement -> element.ComposeUI(enabled)
    }
}

private fun Modifier.formVerticalPadding(
    index: Int,
    maxIndex: Int,
    vertical: Dp,
) = formVerticalPadding(
    index = index,
    maxIndex = maxIndex,
    top = vertical,
    bottom = vertical
)

private fun Modifier.formVerticalPadding(
    index: Int,
    maxIndex: Int,
    top: Dp,
    bottom: Dp,
    start: Dp = 0.dp,
    end: Dp = 0.dp,
) = when {
    maxIndex == 0 -> this.padding(top = 0.dp, bottom = 0.dp, start = start, end = end)
    index == 0 -> this.padding(top = 0.dp, bottom = bottom, start = start, end = end)
    index == maxIndex -> this.padding(top = top, bottom = 0.dp, start = start, end = end)
    else -> this.padding(top = top, bottom = bottom, start = start, end = end)
}

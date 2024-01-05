package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.flow.Flow

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FormUI(
    hiddenIdentifiersFlow: Flow<Set<IdentifierSpec>>,
    enabledFlow: Flow<Boolean>,
    elementsFlow: Flow<List<FormElement>>,
    lastTextFieldIdentifierFlow: Flow<IdentifierSpec?>,
    modifier: Modifier = Modifier
) {
    val hiddenIdentifiers by hiddenIdentifiersFlow.collectAsState(emptySet())
    val enabled by enabledFlow.collectAsState(true)
    val elements by elementsFlow.collectAsState(emptyList())
    val lastTextFieldIdentifier by lastTextFieldIdentifierFlow.collectAsState(null)

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
                    is EmptyFormElement -> {}
                }
            }
        }
    }
}

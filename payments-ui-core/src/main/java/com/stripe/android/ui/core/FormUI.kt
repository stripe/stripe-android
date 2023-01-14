package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.coroutines.flow.Flow

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FormUI(
    hiddenIdentifiersFlow: Flow<Set<IdentifierSpec>>,
    enabledFlow: Flow<Boolean>,
    elementsFlow: Flow<List<FormElement>?>,
    lastTextFieldIdentifierFlow: Flow<IdentifierSpec?>,
    loadingComposable: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val hiddenIdentifiers by hiddenIdentifiersFlow.collectAsState(emptySet())
    val enabled by enabledFlow.collectAsState(true)
    val elements by elementsFlow.collectAsState(null)
    val lastTextFieldIdentifier by lastTextFieldIdentifierFlow.collectAsState(null)

    FormUI(
        hiddenIdentifiers = hiddenIdentifiers,
        enabled = enabled,
        elements = elements,
        lastTextFieldIdentifier = lastTextFieldIdentifier,
        loadingComposable = loadingComposable,
        modifier = modifier
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FormUI(
    hiddenIdentifiers: Set<IdentifierSpec>,
    enabled: Boolean,
    elements: List<FormElement>?,
    lastTextFieldIdentifier: IdentifierSpec?,
    loadingComposable: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(1f)
    ) {
        elements?.let {
            it.forEachIndexed { _, element ->
                if (!hiddenIdentifiers.contains(element.identifier)) {
//                    when (element) {
//                        is SectionElement -> SectionElementUI(
//                            enabled,
//                            element,
//                            hiddenIdentifiers,
//                            lastTextFieldIdentifier
//                        )
//                        is StaticTextElement -> StaticTextElementUI(element)
//                        is SaveForFutureUseElement -> SaveForFutureUseElementUI(enabled, element)
//                        is AfterpayClearpayHeaderElement -> AfterpayClearpayElementUI(
//                            enabled,
//                            element
//                        )
//                        is AuBecsDebitMandateTextElement -> AuBecsDebitMandateElementUI(element)
//                        is AffirmHeaderElement -> AffirmElementUI()
//                        is MandateTextElement -> MandateTextUI(element)
//                        is CardDetailsSectionElement -> CardDetailsSectionElementUI(
//                            enabled,
//                            element.controller,
//                            hiddenIdentifiers,
//                            lastTextFieldIdentifier
//                        )
//                        is BsbElement -> BsbElementUI(enabled, element, lastTextFieldIdentifier)
//                        is OTPElement -> OTPElementUI(enabled, element)
//                        is EmptyFormElement -> {}
//                    }
                }
            }
        } ?: loadingComposable()
    }
}

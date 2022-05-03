package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.MandateTextUI
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionElementUI
import com.stripe.android.ui.core.elements.StaticElementUI
import com.stripe.android.ui.core.elements.StaticTextElement
import kotlinx.coroutines.flow.Flow

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FormUI(
    hiddenIdentifiersFlow: Flow<List<IdentifierSpec>>,
    enabledFlow: Flow<Boolean>,
    elementsFlow: Flow<List<FormElement>?>,
    lastTextFieldIdentifierFlow: Flow<IdentifierSpec?>,
    loadingComposable: @Composable ColumnScope.() -> Unit
) {
    val hiddenIdentifiers by hiddenIdentifiersFlow.collectAsState(emptyList())
    val enabled by enabledFlow.collectAsState(true)
    val elements by elementsFlow.collectAsState(null)
    val lastTextFieldIdentifier by lastTextFieldIdentifierFlow.collectAsState(null)

    Column(
        modifier = Modifier.fillMaxWidth(1f)
    ) {
        elements?.let {
            it.forEachIndexed { _, element ->
                if (!hiddenIdentifiers.contains(element.identifier)) {
                    when (element) {
                        is SectionElement -> SectionElementUI(
                            enabled,
                            element,
                            hiddenIdentifiers,
                            lastTextFieldIdentifier
                        )
                        is StaticTextElement -> StaticElementUI(element)
                        is SaveForFutureUseElement -> SaveForFutureUseElementUI(enabled, element)
                        is AfterpayClearpayHeaderElement -> AfterpayClearpayElementUI(
                            enabled,
                            element
                        )
                        is AuBecsDebitMandateTextElement -> AuBecsDebitMandateElementUI(element)
                        is AffirmHeaderElement -> AffirmElementUI()
                        is MandateTextElement -> MandateTextUI(element)
                        is CardDetailsSectionElement -> CardDetailsSectionElementUI(
                            enabled, element.controller, hiddenIdentifiers
                        )
                        is BsbElement -> BsbElementUI(enabled, element, lastTextFieldIdentifier)
                        is EmptyFormElement -> {}
                    }
                }
            }
        } ?: loadingComposable()
    }
}

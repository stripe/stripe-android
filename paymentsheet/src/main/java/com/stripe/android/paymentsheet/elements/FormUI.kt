package com.stripe.android.paymentsheet.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.forms.FormViewModel
import kotlinx.coroutines.flow.Flow

@Composable
internal fun Form(formViewModel: FormViewModel) {
    FormInternal(
        formViewModel.hiddenIdentifiers,
        formViewModel.enabled,
        formViewModel.elements
    )
}

@Composable
internal fun FormInternal(
    hiddenIdentifiersFlow: Flow<List<IdentifierSpec>>,
    enabledFlow: Flow<Boolean>,
    elements: List<FormElement>
) {
    val hiddenIdentifiers by hiddenIdentifiersFlow.collectAsState(
        null
    )
    val enabled by enabledFlow.collectAsState(true)

    hiddenIdentifiers?.let {
        Column(
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            elements.forEach { element ->
                if (hiddenIdentifiers?.contains(element.identifier) == false) {
                    when (element) {
                        is SectionElement -> SectionElementUI(enabled, element, hiddenIdentifiers)
                        is MandateTextElement -> MandateElementUI(element)
                        is SaveForFutureUseElement -> SaveForFutureUseElementUI(enabled, element)
                        is AfterpayClearpayHeaderElement -> AfterpayClearpayElementUI(
                            enabled,
                            element
                        )
                    }
                }
            }
        }
    }
}

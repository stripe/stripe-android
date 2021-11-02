package com.stripe.android.paymentsheet.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.asLiveData
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
    val hiddenIdentifiers by hiddenIdentifiersFlow.asLiveData().observeAsState(
        null
    )
    val enabled by enabledFlow.asLiveData().observeAsState(true)

    if (hiddenIdentifiers != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
        ) {
            elements.forEach { element ->
                if (hiddenIdentifiers?.contains(element.identifier) == false) {
                    when (element) {
                        is SectionElement -> SectionElementUI(enabled, element, hiddenIdentifiers)
                        is MandateTextElement -> MandateElementUI(element)
                        is SaveForFutureUseElement -> SaveForFutureUseElementUI(enabled, element)
                        is AfterpayClearpayHeaderElement -> AfterpayClearpayHeaderElementUI(
                            enabled,
                            element
                        )
                    }
                }
            }
        }
    }
}

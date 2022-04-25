package com.stripe.android.paymentsheet.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.AffirmElementUI
import com.stripe.android.ui.core.elements.AffirmHeaderElement
import com.stripe.android.ui.core.elements.AfterpayClearpayElementUI
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement
import com.stripe.android.ui.core.elements.AuBecsDebitMandateElementUI
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextElement
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.CardDetailsSectionElementUI
import com.stripe.android.ui.core.elements.BsbElement
import com.stripe.android.ui.core.elements.BsbElementUI
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
import com.stripe.android.ui.core.shouldUseDarkDynamicColor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow

@FlowPreview
@Composable
internal fun Form(formViewModel: FormViewModel) {
    FormInternal(
        formViewModel.hiddenIdentifiers,
        formViewModel.enabled,
        formViewModel.elements,
        formViewModel.lastTextFieldIdentifier
    )
}

@Composable
internal fun FormInternal(
    hiddenIdentifiersFlow: Flow<List<IdentifierSpec>>,
    enabledFlow: Flow<Boolean>,
    elementsFlow: Flow<List<FormElement>?>,
    lastTextFieldIdentifierFlow: Flow<IdentifierSpec?>
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
        } ?: Row(
            modifier = Modifier
                .height(
                    dimensionResource(R.dimen.stripe_paymentsheet_loading_container_height)
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val isDark = PaymentsTheme.colors.material.surface.shouldUseDarkDynamicColor()
            CircularProgressIndicator(
                modifier = Modifier.size(
                    dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_size)
                ),
                color = if (isDark) Color.Black else Color.White,
                strokeWidth = dimensionResource(
                    R.dimen.stripe_paymentsheet_loading_indicator_stroke_width
                )
            )
        }
    }
}

package com.stripe.android.paymentsheet.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.IdentifierSpec
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
    FormUI(
        hiddenIdentifiersFlow,
        enabledFlow,
        elementsFlow,
        lastTextFieldIdentifierFlow
    ) {
        Loading()
    }
}

@Composable
private fun Loading() {
    Row(
        modifier = Modifier
            .height(
                dimensionResource(R.dimen.stripe_paymentsheet_loading_container_height)
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val isDark = MaterialTheme.colors.surface.shouldUseDarkDynamicColor()
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

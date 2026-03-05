package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun ColumnScope.TapToAddCollectCvcScreen(
    state: TapToAddCollectCvcInteractor.State,
    onPrimaryButtonPress: () -> Unit,
) {
    TapToAddCardLayout(
        cardBrand = state.cardBrand,
        last4 = state.last4,
        title = state.title.resolve(),
    ) {
        with(state.form) {
            FormUI(
                elements = elements,
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null,
                enabled = enabled,
            )
        }

        Spacer(Modifier.size(10.dp))

        with(state.primaryButton) {
            PrimaryButton(
                label = label.resolve(),
                locked = false,
                enabled = enabled,
                processingState = PrimaryButtonProcessingState.Idle(null),
                onProcessingCompleted = {},
                onClick = onPrimaryButtonPress,
            )
        }

        Spacer(Modifier.size(10.dp))
    }
}

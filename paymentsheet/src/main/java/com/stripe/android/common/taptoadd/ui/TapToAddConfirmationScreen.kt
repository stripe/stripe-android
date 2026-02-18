package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun TapToAddConfirmationScreen(
    state: TapToAddConfirmationInteractor.State,
    onPrimaryButtonPress: () -> Unit,
    onComplete: () -> Unit,
) {
    TapToAddCardLayout {
        TapToAddCard(state.cardBrand, state.last4)

        Spacer(Modifier.size(20.dp))

        Text(
            text = state.title.resolve(),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.h4.copy(
                fontWeight = FontWeight.Normal,
            ),
        )

        Spacer(Modifier.size(20.dp))

        with(state.form) {
            if (elements.isNotEmpty()) {
                FormUI(
                    elements = elements,
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null,
                    enabled = enabled,
                )

                Spacer(Modifier.size(10.dp))
            }
        }

        with(state.primaryButton) {
            PrimaryButton(
                label = label.resolve(),
                locked = locked,
                enabled = true,
                processingState = when (this.state) {
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle ->
                        PrimaryButtonProcessingState.Idle(null)
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing ->
                        PrimaryButtonProcessingState.Processing
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Complete ->
                        PrimaryButtonProcessingState.Completed
                },
                onProcessingCompleted = onComplete,
                onClick = onPrimaryButtonPress,
            )
        }

        Spacer(Modifier.size(10.dp))

        state.error?.let { error ->
            ErrorMessage(
                error = error.resolve(),
            )
        }
    }
}

package com.stripe.android.common.taptoadd.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun ColumnScope.TapToAddConfirmationScreen(
    state: TapToAddConfirmationInteractor.State,
    onPrimaryButtonPress: () -> Unit,
    onProcessingComplete: () -> Unit,
) {
    val focusRequester = remember {
        FocusRequester()
    }

    if (!LocalInspectionMode.current) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    TapToAddCardLayout(
        cardBrand = state.cardBrand,
        last4 = state.last4,
        title = state.title?.resolve(),
    ) {
        with(state.form) {
            if (elements.isNotEmpty()) {
                FormUI(
                    modifier = Modifier.focusRequester(focusRequester),
                    elements = elements,
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null,
                    enabled = enabled,
                )

                Spacer(Modifier.size(10.dp))
            }
        }

        state.error?.let { error ->
            ErrorMessage(
                error = error.resolve(),
            )

            Spacer(Modifier.size(10.dp))
        }

        with(state.primaryButton) {
            PrimaryButton(
                modifier = Modifier.testTag(TAP_TO_ADD_CONFIRMATION_PRIMARY_BUTTON),
                label = label.resolve(),
                locked = locked,
                enabled = enabled,
                processingState = when (this@with.state) {
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle ->
                        PrimaryButtonProcessingState.Idle(null)
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing ->
                        PrimaryButtonProcessingState.Processing
                    TapToAddConfirmationInteractor.State.PrimaryButton.State.Success ->
                        PrimaryButtonProcessingState.Completed
                },
                onProcessingCompleted = onProcessingComplete,
                onClick = onPrimaryButtonPress,
            )
        }

        Spacer(Modifier.size(10.dp))
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TAP_TO_ADD_CONFIRMATION_PRIMARY_BUTTON = "TAP_TO_ADD_CONFIRMATION_PRIMARY_BUTTON"

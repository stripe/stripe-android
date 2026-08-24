package com.stripe.android.paymentelement.embedded.form

import androidx.annotation.RestrictTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityStateHolder
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.DismissKeyboardOnProcessing
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeFormInsets
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun FormScreenContent(
    interactor: VerticalModeFormInteractor,
    onClick: () -> Unit,
    onProcessingCompleted: () -> Unit,
    state: SheetActivityStateHolder.State,
    onPrimaryButtonDisabledClick: () -> Unit,
) {
    val interactorState by interactor.state.collectAsState()

    DismissKeyboardOnProcessing(interactorState.isProcessing)

    VerticalModeFormUI(interactor = interactor, showsWalletHeader = false)
    USBankAccountMandate(state)
    FormActivityError(state)
    Spacer(Modifier.height(40.dp))
    FormActivityPrimaryButton(
        state = state,
        onClick = onClick,
        onDisabledClick = onPrimaryButtonDisabledClick,
        onProcessingCompleted = onProcessingCompleted,
    )
    PaymentSheetContentPadding()
}

@Composable
internal fun USBankAccountMandate(
    state: SheetActivityStateHolder.State
) {
    state.mandateText?.let {
        Mandate(
            mandateText = it.resolve(),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(MaterialTheme.stripeFormInsets.getOuterFormInsets())
        )
    }
}

@Composable
internal fun FormActivityError(
    state: SheetActivityStateHolder.State
) {
    state.error?.let {
        ErrorMessage(
            error = it.resolve(),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(MaterialTheme.stripeFormInsets.getOuterFormInsets())
        )
    }
}

@Composable
internal fun FormActivityPrimaryButton(
    state: SheetActivityStateHolder.State,
    onDisabledClick: () -> Unit,
    onClick: () -> Unit,
    onProcessingCompleted: () -> Unit = {},
) {
    Box(
        modifier = Modifier.padding(MaterialTheme.stripeFormInsets.getOuterFormInsets())
    ) {
        PrimaryButton(
            modifier = Modifier.testTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON),
            label = state.primaryButtonLabel.resolve(),
            locked = state.shouldDisplayLockIcon,
            enabled = state.isEnabled,
            onClick = onClick,
            onProcessingCompleted = onProcessingCompleted,
            processingState = state.processingState
        )
        if (!state.isEnabled && !state.isProcessing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onDisabledClick() }
                    }
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON = "EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON"

package com.stripe.android.link.ui.cardedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun CardEditScreen(
    viewModel: CardEditViewModel
) {
    val state by viewModel.viewState.collectAsState()
    val formElements by viewModel.formElements.collectAsState()
    val primaryButtonEnabled by viewModel.primaryButtonEnabled.collectAsState()

    if (formElements.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    CardEditBody(
        state = state,
        primaryButtonEnabled = primaryButtonEnabled,
        onSetAsDefaultClick = {},
        onPrimaryButtonClick = {},
        onCancelClick = {}
    ) {
        FormUI(
            hiddenIdentifiers = emptySet(),
            enabled = true,
            elements = formElements,
            lastTextFieldIdentifier = null
        )
    }
}

@Composable
internal fun CardEditBody(
    state: CardEditState,
    primaryButtonEnabled: Boolean,
    onSetAsDefaultClick: (Boolean) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onCancelClick: () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit
) {
    ScrollableTopLevelColumn {
        Text(
            text = stringResource(R.string.stripe_wallet_update_card),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
        StripeThemeForLink {
            formContent()

            Spacer(modifier = Modifier.height(8.dp))

            DefaultPaymentMethodCheckbox(
                setAsDefaultChecked = state.setAsDefault,
                isDefault = state.isDefault,
                isProcessing = state.isProcessing,
                onSetAsDefaultClick = onSetAsDefaultClick,
            )
        }

        AnimatedVisibility(visible = state.errorMessage != null) {
            ErrorText(
                text = state.errorMessage?.resolve(LocalContext.current).orEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        PrimaryButton(
            label = stringResource(R.string.stripe_wallet_update_card),
            state = when {
                state.isProcessing -> PrimaryButtonState.Processing
                primaryButtonEnabled -> PrimaryButtonState.Enabled
                else -> PrimaryButtonState.Disabled
            },
            onButtonClick = onPrimaryButtonClick
        )
//        SecondaryButton(
//            enabled = !isProcessing,
//            label = stringResource(id = com.stripe.android.R.string.stripe_cancel),
//            onClick = onCancelClick
//        )
    }
}

@Composable
private fun DefaultPaymentMethodCheckbox(
    setAsDefaultChecked: Boolean,
    isDefault: Boolean,
    isProcessing: Boolean,
    onSetAsDefaultClick: (Boolean) -> Unit,
) {
    val isChecked = isDefault || setAsDefaultChecked
    val canCheck = !isDefault && !isProcessing

    CheckboxElementUI(
        automationTestTag = "DEFAULT_PAYMENT_METHOD_CHECKBOX_TAG",
        isChecked = isChecked,
        label = stringResource(R.string.stripe_pm_set_as_default),
        isEnabled = canCheck,
        onValueChange = {
            onSetAsDefaultClick(!setAsDefaultChecked)
        }
    )
}

package com.stripe.android.link.ui.cardedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry

@Composable
internal fun CardEditScreen(
    viewModel: CardEditViewModel
) {
    val state by viewModel.state.collectAsState()
    CardEditBody(
        state = state,
        setAsDefault = {},
        onUpdateClicked = {},
        onCancelClicked = {}
    )
}

internal const val DEFAULT_PAYMENT_METHOD_CHECKBOX_TAG = "DEFAULT_PAYMENT_METHOD_CHECKBOX"

@Composable
internal fun CardEditBody(
    state: CardEditState,
    setAsDefault: (Boolean) -> Unit,
    onUpdateClicked: (Map<IdentifierSpec, FormFieldEntry>) -> Unit,
    onCancelClicked: () -> Unit
) {

    if (state.formData == null) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        CardEditBody(
            formData = state.formData,
            isProcessing = state.isProcessing,
            isDefault = state.isDefault,
            setAsDefaultChecked = state.setAsDefault,
            primaryButtonEnabled = false,
            errorMessage = state.errorMessage,
            onSetAsDefaultClick = setAsDefault,
            onPrimaryButtonClick = {
            },
            onCancelClick = onCancelClicked
        )
    }
}

@Composable
internal fun CardEditBody(
    formData: FormData,
    isProcessing: Boolean,
    isDefault: Boolean,
    setAsDefaultChecked: Boolean,
    primaryButtonEnabled: Boolean,
    errorMessage: ResolvableString?,
    onSetAsDefaultClick: (Boolean) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val context = LocalContext.current

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
            FormElement(
                selectedPaymentMethodCode = CardDefinition.type.code,
                formArguments = formData.formArguments,
                enabled = true,
                formElements = formData.formElements,
                usBankAccountFormArguments = formData.usBankAccountFormArguments,
                horizontalPadding = 0.dp,
                onFormFieldValuesChanged = {},
                onInteractionEvent = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            DefaultPaymentMethodCheckbox(
                setAsDefaultChecked = setAsDefaultChecked,
                isDefault = isDefault,
                isProcessing = isProcessing,
                onSetAsDefaultClick = onSetAsDefaultClick,
            )
        }

        AnimatedVisibility(visible = errorMessage != null) {
            ErrorText(
                text = errorMessage?.resolve(context).orEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        PrimaryButton(
            label = stringResource(R.string.stripe_wallet_update_card),
            state = when {
                isProcessing -> PrimaryButtonState.Processing
                primaryButtonEnabled -> PrimaryButtonState.Enabled
                else -> PrimaryButtonState.Disabled
            },
            onButtonClick = onPrimaryButtonClick
        )
        SecondaryButton(
            enabled = !isProcessing,
            label = "Cancel",
            onClick = onCancelClick
        )
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
        automationTestTag = DEFAULT_PAYMENT_METHOD_CHECKBOX_TAG,
        isChecked = isChecked,
        label = stringResource(R.string.stripe_pm_set_as_default),
        isEnabled = canCheck,
        onValueChange = {
            onSetAsDefaultClick(!setAsDefaultChecked)
        }
    )
}

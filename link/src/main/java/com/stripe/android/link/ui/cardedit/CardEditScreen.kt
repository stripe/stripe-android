package com.stripe.android.link.ui.cardedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.R
import com.stripe.android.ui.core.injection.NonFallbackInjector
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.PaymentsThemeForLink
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.link.ui.forms.Form

@Preview
@Composable
internal fun CardEditPreview() {
    DefaultLinkTheme {
        Surface {
            CardEditBody(
                isProcessing = false,
                isDefault = false,
                setAsDefaultChecked = false,
                primaryButtonEnabled = true,
                errorMessage = null,
                onSetAsDefaultClick = {},
                onPrimaryButtonClick = {},
                onCancelClick = {},
                formContent = {}
            )
        }
    }
}

@Composable
internal fun CardEditBody(
    linkAccount: LinkAccount,
    injector: NonFallbackInjector,
    paymentDetailsId: String
) {
    val viewModel: CardEditViewModel = viewModel(
        factory = CardEditViewModel.Factory(linkAccount, injector, paymentDetailsId)
    )

    val formController by viewModel.formController.collectAsState()

    if (formController == null) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        formController?.let {
            val formValues by it.completeFormValues.collectAsState(null)
            val isProcessing by viewModel.isProcessing.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val setAsDefault by viewModel.setAsDefault.collectAsState()

            CardEditBody(
                isProcessing = isProcessing,
                isDefault = viewModel.isDefault,
                setAsDefaultChecked = setAsDefault,
                primaryButtonEnabled = formValues != null,
                errorMessage = errorMessage,
                onSetAsDefaultClick = viewModel::setAsDefault,
                onPrimaryButtonClick = {
                    formValues?.let {
                        viewModel.updateCard(it)
                    }
                },
                onCancelClick = viewModel::dismiss
            ) {
                Form(
                    it,
                    viewModel.isEnabled
                )
            }
        }
    }
}

@Composable
internal fun CardEditBody(
    isProcessing: Boolean,
    isDefault: Boolean,
    setAsDefaultChecked: Boolean,
    primaryButtonEnabled: Boolean,
    errorMessage: ErrorMessage?,
    onSetAsDefaultClick: (Boolean) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onCancelClick: () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit
) {
    ScrollableTopLevelColumn {
        Text(
            text = stringResource(R.string.wallet_update_card),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
        PaymentsThemeForLink {
            formContent()
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isDefault) {
            Text(
                text = stringResource(R.string.pm_your_default),
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.linkColors.disabledText
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clickable {
                        onSetAsDefaultClick(!setAsDefaultChecked)
                    }
            ) {
                Checkbox(
                    checked = setAsDefaultChecked,
                    onCheckedChange = null, // needs to be null for accessibility on row click to work
                    modifier = Modifier.padding(end = 8.dp),
                    enabled = !isProcessing,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary,
                        uncheckedColor = MaterialTheme.linkColors.disabledText
                    )
                )
                Text(
                    text = stringResource(R.string.pm_set_as_default),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onPrimary
                )
            }
        }
        errorMessage?.let {
            ErrorText(text = it.getMessage(LocalContext.current.resources))
        }
        PrimaryButton(
            label = stringResource(R.string.wallet_update_card),
            state = when {
                isProcessing -> PrimaryButtonState.Processing
                primaryButtonEnabled -> PrimaryButtonState.Enabled
                else -> PrimaryButtonState.Disabled
            },
            onButtonClick = onPrimaryButtonClick
        )
        SecondaryButton(
            enabled = !isProcessing,
            label = stringResource(id = R.string.cancel),
            onClick = onCancelClick
        )
    }
}

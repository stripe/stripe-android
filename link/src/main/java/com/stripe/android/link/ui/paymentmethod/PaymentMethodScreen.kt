package com.stripe.android.link.ui.paymentmethod

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.R
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.PaymentsThemeForLink
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PayAnotherWayButton
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.forms.Form
import com.stripe.android.link.ui.primaryButtonLabel

@Preview
@Composable
private fun PaymentMethodBodyPreview() {
    DefaultLinkTheme {
        Surface {
            PaymentMethodBody(
                isProcessing = false,
                primaryButtonLabel = "Pay $10.99",
                primaryButtonEnabled = true,
                errorMessage = null,
                onPrimaryButtonClick = {},
                onPayAnotherWayClick = {},
            ) {}
        }
    }
}

@Composable
internal fun PaymentMethodBody(
    linkAccount: LinkAccount,
    injector: NonFallbackInjector
) {
    val viewModel: PaymentMethodViewModel = viewModel(
        factory = PaymentMethodViewModel.Factory(linkAccount, injector)
    )

    val formValues by viewModel.formController.completeFormValues.collectAsState(null)
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    PaymentMethodBody(
        isProcessing = isProcessing,
        primaryButtonLabel = primaryButtonLabel(viewModel.args, LocalContext.current.resources),
        primaryButtonEnabled = formValues != null,
        errorMessage = errorMessage,
        onPrimaryButtonClick = {
            formValues?.let {
                viewModel.startPayment(it)
            }
        },
        onPayAnotherWayClick = viewModel::payAnotherWay
    ) {
        Form(
            viewModel.formController,
            viewModel.isEnabled
        )
    }
}

@Composable
internal fun PaymentMethodBody(
    isProcessing: Boolean,
    primaryButtonLabel: String,
    primaryButtonEnabled: Boolean,
    errorMessage: ErrorMessage?,
    onPrimaryButtonClick: () -> Unit,
    onPayAnotherWayClick: () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit
) {
    ScrollableTopLevelColumn {
        Text(
            text = stringResource(R.string.pm_add_new_card),
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
        errorMessage?.let {
            ErrorText(text = it.getMessage(LocalContext.current.resources))
        }
        PrimaryButton(
            label = primaryButtonLabel,
            state = when {
                isProcessing -> PrimaryButtonState.Processing
                primaryButtonEnabled -> PrimaryButtonState.Enabled
                else -> PrimaryButtonState.Disabled
            },
            icon = R.drawable.stripe_ic_lock,
            onButtonClick = onPrimaryButtonClick
        )
        PayAnotherWayButton(
            enabled = !isProcessing,
            onClick = onPayAnotherWayClick
        )
    }
}

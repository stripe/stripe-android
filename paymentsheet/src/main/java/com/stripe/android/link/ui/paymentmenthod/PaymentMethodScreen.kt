package com.stripe.android.link.ui.paymentmenthod

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.ui.PaymentMethodForm
import com.stripe.android.uicore.utils.collectAsState
import java.util.UUID

@Composable
internal fun PaymentMethodScreen(
    viewModel: PaymentMethodViewModel,
    onCancelClicked: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    PaymentMethodBody(
        state = state,
        onFormFieldValuesChanged = viewModel::formValuesChanged,
        onPayClicked = viewModel::onPayClicked,
        onCancelClicked = onCancelClicked
    )
}

@Composable
internal fun PaymentMethodBody(
    state: PaymentMethodState,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    onPayClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    val context = LocalContext.current
    val uuid = rememberSaveable { UUID.randomUUID().toString() }

    ScrollableTopLevelColumn {
        Text(
            modifier = Modifier
                .padding(bottom = 32.dp),
            text = R.string.stripe_add_payment_method.resolvableString.resolve(context),
            style = MaterialTheme.typography.h2
        )

        StripeThemeForLink {
            PaymentMethodForm(
                uuid = uuid,
                args = state.formArguments,
                enabled = true,
                onFormFieldValuesChanged = onFormFieldValuesChanged,
                formElements = state.formElements,
            )
        }

        AnimatedVisibility(
            visible = state.errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ErrorText(
                modifier = Modifier
                    .testTag(PAYMENT_METHOD_ERROR_TAG)
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                text = state.errorMessage?.resolve(context).orEmpty()
            )
        }

        PrimaryButton(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            label = state.primaryButtonLabel.resolve(context),
            state = state.primaryButtonState,
            onButtonClick = onPayClicked
        )

        SecondaryButton(
            modifier = Modifier.padding(bottom = 16.dp),
            label = stringResource(com.stripe.android.R.string.stripe_cancel),
            enabled = true,
            onClick = onCancelClicked
        )
    }
}

internal const val PAYMENT_METHOD_ERROR_TAG = "payment_method_error_tag"

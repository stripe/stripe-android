package com.stripe.android.link.ui.paymentmenthod

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.ui.PaymentMethodForm
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import java.util.UUID

@Composable
internal fun PaymentMethodScreen(
    viewModel: PaymentMethodViewModel,
) {
    val state by viewModel.state.collectAsState()

    PaymentMethodBody(
        state = state,
        onFormFieldValuesChanged = viewModel::formValuesChanged,
        onPayClicked = viewModel::onPayClicked,
        onDisabledPayClicked = viewModel::onDisabledPayClicked,
    )
}

@Composable
internal fun PaymentMethodBody(
    state: PaymentMethodState,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    onPayClicked: () -> Unit,
    onDisabledPayClicked: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val uuid = rememberSaveable { UUID.randomUUID().toString() }

    ScrollableTopLevelColumn {
        StripeThemeForLink {
            PaymentMethodForm(
                uuid = uuid,
                args = state.formArguments,
                enabled = true,
                onFormFieldValuesChanged = onFormFieldValuesChanged,
                formElements = state.formUiElements,
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
                text = state.errorMessage?.resolve().orEmpty()
            )
        }

        PrimaryButton(
            modifier = Modifier.padding(vertical = 16.dp),
            label = state.primaryButtonLabel.resolve(),
            state = state.primaryButtonState,
            allowedDisabledClicks = true,
            onDisabledButtonClick = onDisabledPayClicked,
            onButtonClick = {
                focusManager.clearFocus()
                onPayClicked()
            },
        )
    }
}

internal const val PAYMENT_METHOD_ERROR_TAG = "payment_method_error_tag"

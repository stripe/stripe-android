package com.stripe.android.link.ui.paymentmenthod

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.paymentsheet.model.label
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.paymentsheet.ui.PaymentMethodForm
import com.stripe.android.uicore.utils.collectAsState
import java.util.UUID

@Composable
internal fun PaymentMethodScreen(
    viewModel: PaymentMethodViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val uuid = rememberSaveable { UUID.randomUUID().toString() }

    ScrollableTopLevelColumn {
        PaymentMethodForm(
            uuid = uuid,
            args = state.formArguments,
            enabled = true,
            onFormFieldValuesChanged = { formValues ->
                viewModel.formValuesChanged(formValues)
            },
            formElements = state.formElements,
        )

        Text(state.paymentSelection?.label?.resolve(context) ?: "null")

        PrimaryButton(
            label = state.primaryButtonLabel.resolve(context),
            state = state.primaryButtonState,
            onButtonClick = viewModel::onPayClicked
        )
    }
}

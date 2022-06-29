package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.ui.core.injection.NonFallbackInjector

@Composable
internal fun InputAddressScreen(
    collectedAddress: ShippingAddress?,
    primaryButtonEnabled: Boolean,
    onPrimaryButtonClick: () -> Unit,
    onCloseClick: () -> Unit,
    onEnterManuallyClick: () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit
) {
    Column {
        AddressOptionsAppBar(
            isRootScreen = true,
            onButtonClick = { onCloseClick() }
        )
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxHeight()
        ) {
            Text(
                "Shipping Address",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            formContent()
            if (collectedAddress == null) {
                Button(
                    onClick = { onEnterManuallyClick() },
                    content = { Text(text = "Enter manually") }
                )
            }

            AddressElementPrimaryButton(isEnabled = primaryButtonEnabled) {
                onPrimaryButtonClick()
            }
        }
    }
}

@Composable
internal fun InputAddressScreen(
    injector: NonFallbackInjector
) {
    val viewModel: InputAddressViewModel = viewModel(
        factory = InputAddressViewModel.Factory(
            injector
        )
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
            val completeValues by it.completeFormValues.collectAsState(null)
            val collectedAddress by viewModel.collectedAddress.collectAsState()

            InputAddressScreen(
                collectedAddress = collectedAddress,
                primaryButtonEnabled = completeValues != null,
                onPrimaryButtonClick = { viewModel.clickPrimaryButton() },
                onCloseClick = { viewModel.navigator.dismiss() },
                onEnterManuallyClick = { viewModel.expandAddressForm() },
                formContent = {
                    FormUI(
                        it.hiddenIdentifiers,
                        viewModel.formEnabled,
                        it.elements,
                        it.lastTextFieldIdentifier
                    ) {
                        CircularProgressIndicator()
                    }
                }
            )
        }
    }
}

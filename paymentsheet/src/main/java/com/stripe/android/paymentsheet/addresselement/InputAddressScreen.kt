package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.injection.NonFallbackInjector

@Composable
internal fun InputAddressScreen(
    primaryButtonEnabled: Boolean,
    primaryButtonText: String,
    title: String,
    onPrimaryButtonClick: () -> Unit,
    onCloseClick: () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colors.surface)
    ) {
        AddressOptionsAppBar(
            isRootScreen = true,
            onButtonClick = { onCloseClick() }
        )
        Column(
            Modifier
                .padding(horizontal = 20.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            formContent()
            AddressElementPrimaryButton(
                isEnabled = primaryButtonEnabled,
                text = primaryButtonText
            ) {
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
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator()
        }
    } else {
        formController?.let {
            val completeValues by it.completeFormValues.collectAsState(null)
            val buttonText = viewModel.args.config?.buttonTitle ?: stringResource(
                R.string.stripe_paymentsheet_address_element_primary_button
            )
            val titleText = viewModel.args.config?.title ?: stringResource(
                R.string.stripe_paymentsheet_address_element_shipping_address
            )
            InputAddressScreen(
                primaryButtonEnabled = completeValues != null,
                primaryButtonText = buttonText,
                title = titleText,
                onPrimaryButtonClick = { viewModel.clickPrimaryButton() },
                onCloseClick = { viewModel.navigator.dismiss() },
                formContent = {
                    FormUI(
                        it.hiddenIdentifiers,
                        viewModel.formEnabled,
                        it.elements,
                        it.lastTextFieldIdentifier
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            )
        }
    }
}

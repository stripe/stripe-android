package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.utils.collectAsState
import javax.inject.Provider

@Composable
internal fun InputAddressScreen(
    primaryButtonEnabled: Boolean,
    primaryButtonText: String,
    title: String,
    onPrimaryButtonClick: () -> Unit,
    onCloseClick: () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
    checkboxContent: @Composable ColumnScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = Modifier
            .fillMaxHeight()
            .imePadding(),
        backgroundColor = MaterialTheme.colors.surface,
        topBar = {
            AddressOptionsAppBar(
                isRootScreen = true,
                onButtonClick = {
                    focusManager.clearFocus()
                    onCloseClick()
                }
            )
        }
    ) {
        ScrollableColumn(
            modifier = Modifier.padding(it)
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.h4,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                formContent()
                checkboxContent()
                PrimaryButton(
                    isEnabled = primaryButtonEnabled,
                    label = primaryButtonText,
                    onButtonClick = {
                        focusManager.clearFocus()
                        onPrimaryButtonClick()
                    },
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
internal fun InputAddressScreen(
    inputAddressViewModelSubcomponentBuilderProvider: Provider<InputAddressViewModelSubcomponent.Builder>
) {
    val viewModel: InputAddressViewModel = viewModel(
        factory = InputAddressViewModel.Factory(
            inputAddressViewModelSubcomponentBuilderProvider
        )
    )
    val formController by viewModel.formController.collectAsState()
    if (formController == null) {
        LoadingIndicator(
            modifier = Modifier.fillMaxSize()
        )
    } else {
        formController?.let {
            val completeValues by it.completeFormValues.collectAsState()
            val buttonText = viewModel.args.config?.buttonTitle ?: stringResource(
                R.string.stripe_paymentsheet_address_element_primary_button
            )
            val titleText = viewModel.args.config?.title ?: stringResource(
                R.string.stripe_paymentsheet_address_element_shipping_address
            )
            val formEnabled by viewModel.formEnabled.collectAsState()
            val checkboxChecked by viewModel.checkboxChecked.collectAsState()

            InputAddressScreen(
                primaryButtonEnabled = completeValues != null,
                primaryButtonText = buttonText,
                title = titleText,
                onPrimaryButtonClick = {
                    viewModel.clickPrimaryButton(
                        completeValues,
                        checkboxChecked
                    )
                },
                onCloseClick = { viewModel.navigator.dismiss() },
                formContent = {
                    FormUI(
                        hiddenIdentifiersFlow = it.hiddenIdentifiers,
                        enabledFlow = viewModel.formEnabled,
                        elementsFlow = it.elements,
                        lastTextFieldIdentifierFlow = it.lastTextFieldIdentifier,
                    )
                },
                checkboxContent = {
                    viewModel.args.config?.additionalFields?.checkboxLabel?.let { label ->
                        CheckboxElementUI(
                            modifier = Modifier.padding(vertical = 4.dp),
                            isChecked = checkboxChecked,
                            label = label,
                            isEnabled = formEnabled,
                            onValueChange = {
                                viewModel.clickCheckbox(!checkboxChecked)
                            }
                        )
                    }
                }
            )
        }
    }
}

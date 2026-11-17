package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder.State
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeFormInsets
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun InputAddressScreen(
    primaryButtonEnabled: Boolean,
    primaryButtonLoading: Boolean,
    closeButtonEnabled: Boolean,
    primaryButtonText: String,
    title: String,
    onPrimaryButtonClick: () -> Unit,
    onDisabledButtonClick: () -> Unit,
    onCloseClick: () -> Unit,
    topContent: @Composable ColumnScope.() -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit,
    saveError: ResolvableString?,
) {
    val focusManager = LocalFocusManager.current
    val closeButtonFocusRequester = remember { FocusRequester() }
    Scaffold(
        modifier = Modifier
            .fillMaxHeight()
            .imePadding()
            // Outside touch mode, clearing focus refocuses the window. Land on the close button so a
            // prefilled address field isn't focused, which would trigger an autocomplete request.
            .focusProperties { onEnter = { closeButtonFocusRequester.requestFocus() } }
            .focusGroup(),
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        topBar = {
            InputAddressHeader(
                title = title,
                closeButtonEnabled = closeButtonEnabled,
                closeButtonFocusRequester = closeButtonFocusRequester,
                onCloseClick = {
                    focusManager.clearFocus()
                    onCloseClick()
                },
            )
        }
    ) {
        ScrollableColumn(
            modifier = Modifier.padding(it)
        ) {
            Column(
                Modifier
                    .padding(MaterialTheme.stripeFormInsets.getOuterFormInsets())
                    .padding(top = MaterialTheme.stripeFormInsets.top.dp)
                    .padding(bottom = MaterialTheme.stripeFormInsets.bottom.dp)
            ) {
                topContent()
                formContent()
                bottomContent()
                saveError?.let {
                    ErrorMessage(
                        error = it.resolve(),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                PrimaryButton(
                    isEnabled = primaryButtonEnabled,
                    isLoading = primaryButtonLoading,
                    label = primaryButtonText,
                    onButtonClick = {
                        focusManager.clearFocus()
                        onPrimaryButtonClick()
                    },
                    canClickWhileDisabled = !primaryButtonLoading,
                    onDisabledButtonClick = {
                        focusManager.clearFocus()
                        onDisabledButtonClick()
                    },
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun InputAddressHeader(
    title: String,
    closeButtonEnabled: Boolean,
    closeButtonFocusRequester: FocusRequester,
    onCloseClick: () -> Unit,
) {
    val formInsets = MaterialTheme.stripeFormInsets
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = formInsets.start.dp,
                top = 8.dp,
                // Offsets the IconButton's touch target padding so the icon lines up with the form's end edge.
                end = (formInsets.end.dp - 12.dp).coerceAtLeast(0.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h4,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            enabled = closeButtonEnabled,
            onClick = onCloseClick,
            modifier = Modifier.focusRequester(closeButtonFocusRequester),
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                contentDescription = stringResource(R.string.stripe_paymentsheet_close),
                tint = MaterialTheme.stripeColors.appBarIcon,
            )
        }
    }
}

@Composable
internal fun InputAddressScreen(
    viewModel: InputAddressViewModel,
    onCloseClick: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
    }

    val formController = viewModel.addressFormController

    val completeValues by formController.completeFormValues.collectAsState()
    val buttonText = viewModel.args.config?.buttonTitle ?: stringResource(
        R.string.stripe_paymentsheet_address_element_primary_button
    )
    val titleText = viewModel.args.config?.title ?: stringResource(
        R.string.stripe_paymentsheet_address_element_shipping_address
    )
    val formEnabled by viewModel.formEnabled.collectAsState()
    val resultState by viewModel.resultStateHolder.state.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val checkboxChecked by viewModel.checkboxChecked.collectAsState()
    val billingSameAsShippingState by viewModel.shippingSameAsBillingState.collectAsState()

    InputAddressScreen(
        primaryButtonEnabled = completeValues != null && formEnabled,
        primaryButtonLoading = resultState == State.Saving,
        closeButtonEnabled = formEnabled,
        primaryButtonText = buttonText,
        title = titleText,
        onPrimaryButtonClick = {
            viewModel.clickPrimaryButton(
                completeValues,
                checkboxChecked
            )
        },
        onDisabledButtonClick = {
            viewModel.clickPrimaryButton(
                completedFormValues = null,
                checkboxChecked = checkboxChecked
            )
        },
        onCloseClick = onCloseClick,
        topContent = {
            val currentState = billingSameAsShippingState

            if (currentState is InputAddressViewModel.ShippingSameAsBillingState.Show) {
                CheckboxElementUI(
                    modifier = Modifier.padding(bottom = 8.dp),
                    isChecked = currentState.isChecked,
                    label = R.string.stripe_paymentsheet_address_element_use_billing_as_shipping
                        .resolvableString
                        .resolve(),
                    isEnabled = formEnabled,
                    onValueChange = {
                        viewModel.clickBillingSameAsShipping(it)
                    }
                )
            }
        },
        formContent = {
            AddressFormContent(
                formController = formController,
                enabled = formEnabled,
            )
        },
        bottomContent = {
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
        },
        saveError = saveError,
    )
}

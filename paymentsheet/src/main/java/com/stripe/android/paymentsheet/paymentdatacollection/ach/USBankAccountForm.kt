package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.model.PaymentIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection.New
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.AddressElementUI
import com.stripe.android.uicore.elements.H6Text
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElementUI
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SameAsShippingElementUI
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection
import com.stripe.android.uicore.stripeColors
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@Composable
internal fun USBankAccountForm(
    formArgs: FormArguments,
    sheetViewModel: BaseSheetViewModel,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current

    val viewModel = viewModel<USBankAccountFormViewModel>(
        factory = USBankAccountFormViewModel.Factory {
            val initializationMode = (sheetViewModel as? PaymentSheetViewModel)
                ?.args
                ?.initializationMode
            val onBehalfOf = (initializationMode as? PaymentSheet.InitializationMode.DeferredIntent)
                ?.intentConfiguration
                ?.onBehalfOf
            val stripeIntent = sheetViewModel.stripeIntent.value
            USBankAccountFormViewModel.Args(
                formArgs = formArgs,
                isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
                isPaymentFlow = stripeIntent is PaymentIntent,
                stripeIntentId = stripeIntent?.id,
                clientSecret = stripeIntent?.clientSecret,
                onBehalfOf = onBehalfOf,
                savedPaymentMethod = sheetViewModel.newPaymentSelection as? New.USBankAccount,
                shippingDetails = sheetViewModel.config?.shippingDetails,
            )
        },
    )

    SyncViewModels(
        viewModel = viewModel,
        sheetViewModel = sheetViewModel,
    )

    DisposableEffect(Unit) {
        viewModel.register(activityResultRegistryOwner!!)

        onDispose {
            sheetViewModel.resetUSBankPrimaryButton()
            viewModel.onDestroy()
        }
    }

    val currentScreenState by viewModel.currentScreenState.collectAsState()
    val hasRequiredFields by viewModel.requiredFields.collectAsState()
    val lastTextFieldIdentifier by viewModel.lastTextFieldIdentifier.collectAsState(null)

    LaunchedEffect(currentScreenState, hasRequiredFields) {
        sheetViewModel.handleScreenStateChanged(
            context = context,
            screenState = currentScreenState,
            enabled = hasRequiredFields,
            merchantName = viewModel.formattedMerchantName(),
            onPrimaryButtonClick = viewModel::handlePrimaryButtonClick,
        )
    }

    Box(modifier) {
        when (val screenState = currentScreenState) {
            is USBankAccountFormScreenState.BillingDetailsCollection -> {
                BillingDetailsCollectionScreen(
                    formArgs = formArgs,
                    isProcessing = isProcessing,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    phoneController = viewModel.phoneController,
                    addressController = viewModel.addressElement.controller,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    sameAsShippingElement = viewModel.sameAsShippingElement,
                )
            }
            is USBankAccountFormScreenState.MandateCollection -> {
                MandateCollectionScreen(
                    formArgs = formArgs,
                    isProcessing = isProcessing,
                    screenState = screenState,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    phoneController = viewModel.phoneController,
                    addressController = viewModel.addressElement.controller,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    sameAsShippingElement = viewModel.sameAsShippingElement,
                    saveForFutureUseElement = viewModel.saveForFutureUseElement,
                    onRemoveAccount = viewModel::reset,
                )
            }
            is USBankAccountFormScreenState.VerifyWithMicrodeposits -> {
                VerifyWithMicrodepositsScreen(
                    formArgs = formArgs,
                    isProcessing = isProcessing,
                    screenState = screenState,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    phoneController = viewModel.phoneController,
                    addressController = viewModel.addressElement.controller,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    sameAsShippingElement = viewModel.sameAsShippingElement,
                    saveForFutureUseElement = viewModel.saveForFutureUseElement,
                    onRemoveAccount = viewModel::reset,
                )
            }
            is USBankAccountFormScreenState.SavedAccount -> {
                SavedAccountScreen(
                    formArgs = formArgs,
                    isProcessing = isProcessing,
                    screenState = screenState,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    phoneController = viewModel.phoneController,
                    addressController = viewModel.addressElement.controller,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    sameAsShippingElement = viewModel.sameAsShippingElement,
                    saveForFutureUseElement = viewModel.saveForFutureUseElement,
                    onRemoveAccount = viewModel::reset,
                )
            }
        }
    }
}

@Composable
internal fun BillingDetailsCollectionScreen(
    formArgs: FormArguments,
    isProcessing: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
) {
    Column(Modifier.fillMaxWidth()) {
        BillingDetailsForm(
            formArgs = formArgs,
            isProcessing = isProcessing,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
    }
}

@Composable
internal fun MandateCollectionScreen(
    formArgs: FormArguments,
    isProcessing: Boolean,
    screenState: USBankAccountFormScreenState.MandateCollection,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
    saveForFutureUseElement: SaveForFutureUseElement,
    onRemoveAccount: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BillingDetailsForm(
            formArgs = formArgs,
            isProcessing = isProcessing,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
        AccountDetailsForm(
            formArgs = formArgs,
            isProcessing = isProcessing,
            bankName = screenState.paymentAccount.institutionName,
            last4 = screenState.paymentAccount.last4,
            saveForFutureUseElement = saveForFutureUseElement,
            onRemoveAccount = onRemoveAccount,
        )
    }
}

@Composable
internal fun VerifyWithMicrodepositsScreen(
    formArgs: FormArguments,
    isProcessing: Boolean,
    screenState: USBankAccountFormScreenState.VerifyWithMicrodeposits,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
    saveForFutureUseElement: SaveForFutureUseElement,
    onRemoveAccount: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BillingDetailsForm(
            formArgs = formArgs,
            isProcessing = isProcessing,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
        AccountDetailsForm(
            formArgs = formArgs,
            isProcessing = isProcessing,
            bankName = screenState.paymentAccount.bankName,
            last4 = screenState.paymentAccount.last4,
            saveForFutureUseElement = saveForFutureUseElement,
            onRemoveAccount = onRemoveAccount,
        )
    }
}

@Composable
internal fun SavedAccountScreen(
    formArgs: FormArguments,
    isProcessing: Boolean,
    screenState: USBankAccountFormScreenState.SavedAccount,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
    saveForFutureUseElement: SaveForFutureUseElement,
    onRemoveAccount: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BillingDetailsForm(
            formArgs = formArgs,
            isProcessing = isProcessing,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
        AccountDetailsForm(
            formArgs = formArgs,
            isProcessing = isProcessing,
            bankName = screenState.bankName,
            last4 = screenState.last4,
            saveForFutureUseElement = saveForFutureUseElement,
            onRemoveAccount = onRemoveAccount,
        )
    }
}

@Composable
internal fun BillingDetailsForm(
    formArgs: FormArguments,
    isProcessing: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
) {
    Column(Modifier.fillMaxWidth()) {
        H6Text(
            text = stringResource(R.string.stripe_paymentsheet_pay_with_bank_title),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        if (formArgs.billingDetailsCollectionConfiguration.name != CollectionMode.Never) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextFieldSection(
                    textFieldController = nameController,
                    imeAction = ImeAction.Next,
                    enabled = !isProcessing
                )
            }
        }
        if (formArgs.billingDetailsCollectionConfiguration.email != CollectionMode.Never) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextFieldSection(
                    textFieldController = emailController,
                    imeAction = if (lastTextFieldIdentifier == IdentifierSpec.Email) {
                        ImeAction.Done
                    } else {
                        ImeAction.Next
                    },
                    enabled = !isProcessing
                )
            }
        }
        if (formArgs.billingDetailsCollectionConfiguration.phone == CollectionMode.Always) {
            PhoneSection(
                isProcessing = isProcessing,
                phoneController = phoneController,
                imeAction = if (lastTextFieldIdentifier == IdentifierSpec.Phone) {
                    ImeAction.Done
                } else {
                    ImeAction.Next
                },
            )
        }
        if (formArgs.billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full) {
            AddressSection(
                isProcessing = isProcessing,
                addressController = addressController,
                lastTextFieldIdentifier = lastTextFieldIdentifier,
                sameAsShippingElement = sameAsShippingElement,
            )
        }
    }
}

@Suppress("SpreadOperator")
@Composable
private fun PhoneSection(
    isProcessing: Boolean,
    phoneController: PhoneNumberController,
    imeAction: ImeAction,
) {
    val error by phoneController.error.collectAsState(null)

    val sectionErrorString = error?.let {
        it.formatArgs?.let { args ->
            stringResource(
                it.errorMessage,
                *args
            )
        } ?: stringResource(it.errorMessage)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Section(null, sectionErrorString) {
            PhoneNumberElementUI(
                enabled = !isProcessing,
                controller = phoneController,
                imeAction = imeAction,
            )
        }
    }
}

@Suppress("SpreadOperator")
@Composable
private fun AddressSection(
    isProcessing: Boolean,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
) {
    val error by addressController.error.collectAsState(null)

    val sectionErrorString = error?.let {
        it.formatArgs?.let { args ->
            stringResource(
                it.errorMessage,
                *args
            )
        } ?: stringResource(it.errorMessage)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column {
            Section(PaymentsUiCoreR.string.stripe_billing_details, sectionErrorString) {
                AddressElementUI(
                    enabled = !isProcessing,
                    controller = addressController,
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                )
            }
            sameAsShippingElement?.let {
                SameAsShippingElementUI(it.controller)
            }
        }
    }
}

@Composable
private fun AccountDetailsForm(
    formArgs: FormArguments,
    isProcessing: Boolean,
    bankName: String?,
    last4: String?,
    saveForFutureUseElement: SaveForFutureUseElement,
    onRemoveAccount: () -> Unit,
) {
    val openDialog = remember { mutableStateOf(false) }
    val bankIcon = TransformToBankIcon(bankName)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        H6Text(
            text = stringResource(StripeR.string.stripe_title_bank_account),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(bankIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .height(40.dp)
                            .width(56.dp)
                    )
                    Text(
                        text = "$bankName ••••$last4",
                        modifier = Modifier.alpha(if (isProcessing) 0.5f else 1f),
                        color = MaterialTheme.stripeColors.onComponent
                    )
                }
                Image(
                    painter = painterResource(StripeR.drawable.stripe_ic_clear),
                    contentDescription = null,
                    modifier = Modifier
                        .height(20.dp)
                        .width(20.dp)
                        .alpha(if (isProcessing) 0.5f else 1f)
                        .clickable {
                            if (!isProcessing) {
                                openDialog.value = true
                            }
                        }
                )
            }
        }
        if (formArgs.showCheckbox) {
            SaveForFutureUseElementUI(
                enabled = true,
                element = saveForFutureUseElement,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    last4?.let {
        SimpleDialogElementUI(
            openDialog = openDialog,
            titleText = stringResource(
                id = R.string.stripe_paymentsheet_remove_bank_account_title
            ),
            messageText = stringResource(
                id = R.string.stripe_bank_account_ending_in,
                last4
            ),
            confirmText = stringResource(
                id = StripeR.string.stripe_remove
            ),
            dismissText = stringResource(
                id = StripeR.string.stripe_cancel
            ),
            onConfirmListener = {
                openDialog.value = false
                onRemoveAccount()
            },
            onDismissListener = {
                openDialog.value = false
            }
        )
    }
}

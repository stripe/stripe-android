package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection.New
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
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
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_ACCOUNT_DETAILS = "TEST_TAG_ACCOUNT_DETAILS"

@Composable
internal fun USBankAccountForm(
    formArgs: FormArguments,
    usBankAccountFormArgs: USBankAccountFormArguments,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<USBankAccountFormViewModel>(
        factory = USBankAccountFormViewModel.Factory {
            USBankAccountFormViewModel.Args(
                instantDebits = usBankAccountFormArgs.instantDebits,
                linkMode = usBankAccountFormArgs.linkMode,
                formArgs = formArgs,
                hostedSurface = usBankAccountFormArgs.hostedSurface,
                showCheckbox = usBankAccountFormArgs.showCheckbox,
                isCompleteFlow = usBankAccountFormArgs.isCompleteFlow,
                isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
                stripeIntentId = usBankAccountFormArgs.stripeIntentId,
                clientSecret = usBankAccountFormArgs.clientSecret,
                onBehalfOf = usBankAccountFormArgs.onBehalfOf,
                savedPaymentMethod = usBankAccountFormArgs.draftPaymentSelection as? New.USBankAccount,
                shippingDetails = usBankAccountFormArgs.shippingDetails,
            )
        },
    )

    val currentScreenState by viewModel.currentScreenState.collectAsState()
    val lastTextFieldIdentifier by viewModel.lastTextFieldIdentifier.collectAsState()

    USBankAccountEmitters(
        viewModel = viewModel,
        usBankAccountFormArgs = usBankAccountFormArgs,
    )

    Box(modifier) {
        when (val screenState = currentScreenState) {
            is USBankAccountFormScreenState.BillingDetailsCollection -> {
                BillingDetailsCollectionScreen(
                    fieldsState = viewModel.fieldsState,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
                    isProcessing = screenState.isProcessing,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    phoneController = viewModel.phoneController,
                    addressController = viewModel.addressElement.controller,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    sameAsShippingElement = viewModel.sameAsShippingElement,
                )
            }
            is USBankAccountFormScreenState.MandateCollection -> {
                AccountPreviewScreen(
                    fieldsState = viewModel.fieldsState,
                    bankName = screenState.bankName,
                    last4 = screenState.last4,
                    showCheckbox = usBankAccountFormArgs.showCheckbox,
                    isProcessing = screenState.isProcessing,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
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
                AccountPreviewScreen(
                    fieldsState = viewModel.fieldsState,
                    bankName = screenState.paymentAccount.bankName,
                    last4 = screenState.paymentAccount.last4,
                    showCheckbox = usBankAccountFormArgs.showCheckbox,
                    isProcessing = screenState.isProcessing,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
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
                AccountPreviewScreen(
                    fieldsState = viewModel.fieldsState,
                    bankName = screenState.bankName,
                    last4 = screenState.last4,
                    showCheckbox = usBankAccountFormArgs.showCheckbox,
                    isProcessing = screenState.isProcessing,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
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
    fieldsState: BankFormFieldsState,
    isProcessing: Boolean,
    isPaymentFlow: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
) {
    Column(Modifier.fillMaxWidth()) {
        BillingDetailsForm(
            fieldsState = fieldsState,
            isProcessing = isProcessing,
            isPaymentFlow = isPaymentFlow,
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
internal fun AccountPreviewScreen(
    fieldsState: BankFormFieldsState,
    bankName: String?,
    last4: String?,
    showCheckbox: Boolean,
    isProcessing: Boolean,
    isPaymentFlow: Boolean,
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
            fieldsState = fieldsState,
            isProcessing = isProcessing,
            isPaymentFlow = isPaymentFlow,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
        AccountDetailsForm(
            showCheckbox = showCheckbox,
            isProcessing = isProcessing,
            bankName = bankName,
            last4 = last4,
            saveForFutureUseElement = saveForFutureUseElement,
            onRemoveAccount = onRemoveAccount,
        )
    }
}

@Composable
internal fun BillingDetailsForm(
    isProcessing: Boolean,
    isPaymentFlow: Boolean,
    fieldsState: BankFormFieldsState,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
) {
    Column(Modifier.fillMaxWidth()) {
        H6Text(
            text = if (isPaymentFlow) {
                stringResource(R.string.stripe_paymentsheet_pay_with_bank_title)
            } else {
                stringResource(R.string.stripe_paymentsheet_save_bank_title)
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (fieldsState.showNameField) {
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
        if (fieldsState.showEmailField) {
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
        if (fieldsState.showPhoneField) {
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
        if (fieldsState.showAddressFields) {
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
    val error by phoneController.error.collectAsState()

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
    val error by addressController.error.collectAsState()

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
    showCheckbox: Boolean,
    isProcessing: Boolean,
    bankName: String?,
    last4: String?,
    saveForFutureUseElement: SaveForFutureUseElement,
    onRemoveAccount: () -> Unit,
) {
    var openDialog by rememberSaveable { mutableStateOf(false) }
    val bankIcon = remember(bankName) { TransformToBankIcon(bankName) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag(TEST_TAG_ACCOUNT_DETAILS)
    ) {
        H6Text(
            text = stringResource(StripeR.string.stripe_title_bank_account),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = 12.dp)
                    .padding(start = 16.dp)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(bankIcon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )

                    Text(
                        text = "$bankName •••• $last4",
                        modifier = Modifier.alpha(if (isProcessing) 0.5f else 1f),
                        color = MaterialTheme.stripeColors.onComponent,
                    )
                }

                IconButton(
                    enabled = !isProcessing,
                    onClick = { openDialog = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        painter = painterResource(StripeR.drawable.stripe_ic_clear),
                        contentDescription = null,
                    )
                }
            }
        }
        if (showCheckbox) {
            SaveForFutureUseElementUI(
                enabled = true,
                element = saveForFutureUseElement,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (openDialog && last4 != null) {
        SimpleDialogElementUI(
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
            destructive = true,
            onConfirmListener = {
                openDialog = false
                onRemoveAccount()
            },
            onDismissListener = {
                openDialog = false
            }
        )
    }
}

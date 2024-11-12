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
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
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
import com.stripe.android.uicore.elements.TextField
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

    val state by viewModel.currentScreenState.collectAsState()
    val lastTextFieldIdentifier by viewModel.lastTextFieldIdentifier.collectAsState()

    USBankAccountEmitters(
        viewModel = viewModel,
        usBankAccountFormArgs = usBankAccountFormArgs,
    )

    BankAccountForm(
        state = state,
        formArgs = formArgs,
        instantDebits = usBankAccountFormArgs.instantDebits,
        isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
        showCheckbox = usBankAccountFormArgs.showCheckbox,
        nameController = viewModel.nameController,
        emailController = viewModel.emailController,
        phoneController = viewModel.phoneController,
        addressController = viewModel.addressElement.controller,
        lastTextFieldIdentifier = lastTextFieldIdentifier,
        sameAsShippingElement = viewModel.sameAsShippingElement,
        saveForFutureUseElement = viewModel.saveForFutureUseElement,
        onRemoveAccount = viewModel::reset,
        modifier = modifier,
    )
}

@Composable
internal fun BankAccountForm(
    state: BankFormScreenState,
    formArgs: FormArguments,
    instantDebits: Boolean,
    isPaymentFlow: Boolean,
    showCheckbox: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
    saveForFutureUseElement: SaveForFutureUseElement,
    modifier: Modifier = Modifier,
    onRemoveAccount: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        BillingDetailsForm(
            instantDebits = instantDebits,
            formArgs = formArgs,
            isPaymentFlow = isPaymentFlow,
            isProcessing = state.isProcessing,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )

        state.linkedBankAccount?.let { linkedBankAccount ->
            AccountDetailsForm(
                showCheckbox = showCheckbox,
                isProcessing = state.isProcessing,
                bankName = linkedBankAccount.bankName,
                last4 = linkedBankAccount.last4,
                label = linkedBankAccount.label,
                saveForFutureUseElement = saveForFutureUseElement,
                onRemoveAccount = onRemoveAccount,
            )
        }
    }
}

@Composable
private fun BillingDetailsForm(
    instantDebits: Boolean,
    formArgs: FormArguments,
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
        H6Text(
            text = if (isPaymentFlow) {
                stringResource(R.string.stripe_paymentsheet_pay_with_bank_title)
            } else {
                stringResource(R.string.stripe_paymentsheet_save_bank_title)
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        val showName = if (instantDebits) {
            // Only show if we're being forced to
            formArgs.billingDetailsCollectionConfiguration.name == CollectionMode.Always
        } else {
            formArgs.billingDetailsCollectionConfiguration.name != CollectionMode.Never
        }

        if (showName) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextFieldSection(
                    modifier = Modifier.padding(vertical = 8.dp),
                    textFieldController = nameController,
                ) {
                    TextField(
                        textFieldController = nameController,
                        enabled = !isProcessing,
                        imeAction = ImeAction.Next,
                    )
                }
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
                    modifier = Modifier.padding(vertical = 8.dp),
                    textFieldController = emailController,
                ) {
                    TextField(
                        textFieldController = emailController,
                        enabled = !isProcessing,
                        imeAction = if (lastTextFieldIdentifier == IdentifierSpec.Email) {
                            ImeAction.Done
                        } else {
                            ImeAction.Next
                        },
                    )
                }
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
        Section(
            modifier = Modifier.padding(vertical = 8.dp),
            title = null,
            error = sectionErrorString,
        ) {
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
            Section(
                modifier = Modifier.padding(vertical = 8.dp),
                title = PaymentsUiCoreR.string.stripe_billing_details,
                error = sectionErrorString,
            ) {
                AddressElementUI(
                    enabled = !isProcessing,
                    controller = addressController,
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                )
            }
            sameAsShippingElement?.let {
                SameAsShippingElementUI(it.controller, Modifier.padding(vertical = 4.dp))
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
    label: String,
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
                        text = label,
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

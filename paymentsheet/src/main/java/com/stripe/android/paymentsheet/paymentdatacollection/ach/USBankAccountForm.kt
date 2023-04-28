package com.stripe.android.paymentsheet.paymentdatacollection.ach

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.R
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

@Composable
internal fun BillingDetailsCollectionScreen(
    formArgs: FormArguments,
    processing: Boolean,
    screenState: USBankAccountFormScreenState.BillingDetailsCollection,
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
            processing = processing,
            name = screenState.name,
            email = screenState.email,
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
    processing: Boolean,
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
            processing = processing,
            name = screenState.name,
            email = screenState.email,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
        AccountDetailsForm(
            formArgs = formArgs,
            processing = processing,
            bankName = screenState.paymentAccount.institutionName,
            last4 = screenState.paymentAccount.last4,
            saveForFutureUsage = screenState.saveForFutureUsage,
            saveForFutureUseElement = saveForFutureUseElement,
            onRemoveAccount = onRemoveAccount,
        )
    }
}

@Composable
internal fun VerifyWithMicrodepositsScreen(
    formArgs: FormArguments,
    processing: Boolean,
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
            processing = processing,
            name = screenState.name,
            email = screenState.email,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
        AccountDetailsForm(
            formArgs = formArgs,
            processing = processing,
            bankName = screenState.paymentAccount.bankName,
            last4 = screenState.paymentAccount.last4,
            saveForFutureUsage = screenState.saveForFutureUsage,
            saveForFutureUseElement = saveForFutureUseElement,
            onRemoveAccount = onRemoveAccount,
        )
    }
}

@Composable
internal fun SavedAccountScreen(
    formArgs: FormArguments,
    processing: Boolean,
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
            processing = processing,
            name = screenState.name,
            email = screenState.email,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
        )
        AccountDetailsForm(
            formArgs = formArgs,
            processing = processing,
            bankName = screenState.bankName,
            last4 = screenState.last4,
            saveForFutureUsage = screenState.saveForFutureUsage,
            saveForFutureUseElement = saveForFutureUseElement,
            onRemoveAccount = onRemoveAccount,
        )
    }
}

@Composable
internal fun BillingDetailsForm(
    formArgs: FormArguments,
    processing: Boolean,
    name: String,
    email: String?,
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
                    textFieldController = nameController.apply {
                        onRawValueChange(name)
                    },
                    imeAction = ImeAction.Next,
                    enabled = !processing
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
                    textFieldController = emailController.apply {
                        onRawValueChange(email ?: "")
                    },
                    imeAction = if (lastTextFieldIdentifier == IdentifierSpec.Email) {
                        ImeAction.Done
                    } else {
                        ImeAction.Next
                    },
                    enabled = !processing
                )
            }
        }
        if (formArgs.billingDetailsCollectionConfiguration.phone == CollectionMode.Always) {
            PhoneSection(
                processing = processing,
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
                processing = processing,
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
    processing: Boolean,
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
                enabled = !processing,
                controller = phoneController,
                imeAction = imeAction,
            )
        }
    }
}

@Suppress("SpreadOperator")
@Composable
private fun AddressSection(
    processing: Boolean,
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
            Section(R.string.stripe_billing_details, sectionErrorString) {
                AddressElementUI(
                    enabled = !processing,
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
    processing: Boolean,
    bankName: String?,
    last4: String?,
    saveForFutureUsage: Boolean,
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
            text = stringResource(R.string.stripe_title_bank_account),
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
                        modifier = Modifier.alpha(if (processing) 0.5f else 1f),
                        color = MaterialTheme.stripeColors.onComponent
                    )
                }
                Image(
                    painter = painterResource(R.drawable.stripe_ic_clear),
                    contentDescription = null,
                    modifier = Modifier
                        .height(20.dp)
                        .width(20.dp)
                        .alpha(if (processing) 0.5f else 1f)
                        .clickable {
                            if (!processing) {
                                openDialog.value = true
                            }
                        }
                )
            }
        }
        if (formArgs.showCheckbox) {
            SaveForFutureUseElementUI(
                enabled = true,
                element = saveForFutureUseElement.apply {
                    this.controller.onValueChange(saveForFutureUsage)
                },
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
                id = R.string.stripe_remove
            ),
            dismissText = stringResource(
                id = R.string.stripe_cancel
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

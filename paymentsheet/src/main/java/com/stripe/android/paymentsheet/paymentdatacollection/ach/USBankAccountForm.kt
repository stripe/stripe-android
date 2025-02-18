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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection.New
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.BankFormScreenState.PromoBadgeState
import com.stripe.android.paymentsheet.ui.Mandate
import com.stripe.android.paymentsheet.ui.PromoBadge
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
import com.stripe.android.uicore.strings.resolve
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
    enabled: Boolean
) {
    val viewModel = viewModel<USBankAccountFormViewModel>(
        factory = USBankAccountFormViewModel.Factory {
            USBankAccountFormViewModel.Args(
                instantDebits = usBankAccountFormArgs.instantDebits,
                incentive = usBankAccountFormArgs.incentive,
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
    val isEnabled = !state.isProcessing && enabled

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
        enabled = isEnabled
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
    enabled: Boolean,
    onRemoveAccount: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        BillingDetailsForm(
            instantDebits = instantDebits,
            formArgs = formArgs,
            isPaymentFlow = isPaymentFlow,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
            enabled = enabled
        )

        state.linkedBankAccount?.let { linkedBankAccount ->
            AccountDetailsForm(
                modifier = Modifier.padding(top = 16.dp),
                showCheckbox = showCheckbox,
                bankName = linkedBankAccount.bankName,
                last4 = linkedBankAccount.last4,
                promoBadgeState = state.promoBadgeState,
                saveForFutureUseElement = saveForFutureUseElement,
                onRemoveAccount = onRemoveAccount,
                enabled = enabled
            )
        }

        state.promoDisclaimerText?.let {
            PromoDisclaimer(
                promoText = it.resolve(),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun PromoDisclaimer(
    promoText: String,
    modifier: Modifier = Modifier,
) {
    // Not technically a mandate, but we want to use the same style
    Mandate(
        mandateText = promoText,
        modifier = modifier,
    )
}

@Composable
private fun BillingDetailsForm(
    instantDebits: Boolean,
    formArgs: FormArguments,
    enabled: Boolean,
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
                    modifier = Modifier.padding(top = 16.dp),
                    textFieldController = nameController,
                ) {
                    TextField(
                        textFieldController = nameController,
                        enabled = enabled,
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
                    modifier = Modifier.padding(top = 16.dp),
                    textFieldController = emailController,
                ) {
                    TextField(
                        textFieldController = emailController,
                        enabled = enabled,
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
                enabled = enabled,
                phoneController = phoneController,
                imeAction = if (lastTextFieldIdentifier == IdentifierSpec.Phone) {
                    ImeAction.Done
                } else {
                    ImeAction.Next
                },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        if (formArgs.billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full) {
            AddressSection(
                enabled = enabled,
                addressController = addressController,
                lastTextFieldIdentifier = lastTextFieldIdentifier,
                sameAsShippingElement = sameAsShippingElement,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Suppress("SpreadOperator")
@Composable
private fun PhoneSection(
    enabled: Boolean,
    phoneController: PhoneNumberController,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
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
            modifier = modifier,
            title = null,
            error = sectionErrorString,
        ) {
            PhoneNumberElementUI(
                enabled = enabled,
                controller = phoneController,
                imeAction = imeAction,
            )
        }
    }
}

@Suppress("SpreadOperator")
@Composable
private fun AddressSection(
    enabled: Boolean,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
    modifier: Modifier = Modifier,
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
        Column(modifier) {
            Section(
                title = PaymentsUiCoreR.string.stripe_billing_details,
                error = sectionErrorString,
            ) {
                AddressElementUI(
                    enabled = enabled,
                    controller = addressController,
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                )
            }
            sameAsShippingElement?.let {
                SameAsShippingElementUI(it.controller, Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun AccountDetailsForm(
    modifier: Modifier = Modifier,
    showCheckbox: Boolean,
    enabled: Boolean,
    bankName: String?,
    last4: String?,
    promoBadgeState: PromoBadgeState?,
    saveForFutureUseElement: SaveForFutureUseElement,
    onRemoveAccount: () -> Unit,
) {
    var openDialog by rememberSaveable { mutableStateOf(false) }
    val bankIcon = remember(bankName) { TransformToBankIcon(bankName) }

    Column(
        modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_ACCOUNT_DETAILS)
    ) {
        H6Text(
            text = stringResource(StripeR.string.stripe_title_bank_account),
            modifier = Modifier.padding(bottom = 8.dp)
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
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(bankIcon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )

                    Text(
                        text = "$bankName •••• $last4",
                        color = MaterialTheme.stripeColors.onComponent,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .alpha(if (!enabled) 0.5f else 1f)
                            .weight(1f, fill = false),
                    )

                    promoBadgeState?.let { badgeState ->
                        PromoBadge(
                            text = badgeState.promoText,
                            eligible = badgeState.eligible,
                        )
                    }
                }

                IconButton(
                    enabled = enabled,
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

package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Parcelable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.networking.Bank
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection.New
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.uicore.StripeTheme
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
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.parcelize.Parcelize
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@Composable
internal fun USBankAccountForm(
    imageLoader: StripeImageLoader,
    formArgs: FormArguments,
    usBankAccountFormArgs: USBankAccountFormArguments,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<USBankAccountFormViewModel>(
        factory = USBankAccountFormViewModel.Factory {
            USBankAccountFormViewModel.Args(
                instantDebits = usBankAccountFormArgs.instantDebits,
                formArgs = formArgs,
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
                    imageLoader = imageLoader,
                    instantDebits = usBankAccountFormArgs.instantDebits,
                    formArgs = formArgs,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
                    isProcessing = screenState.isProcessing,
                    isTestMode = false,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    phoneController = viewModel.phoneController,
                    addressController = viewModel.addressElement.controller,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    sameAsShippingElement = viewModel.sameAsShippingElement,
                    featuredInstitutions = screenState.featuredInstitutions,
                    onBankSelected = viewModel::handleBankSelected,
                )
            }
            is USBankAccountFormScreenState.MandateCollection -> {
                AccountPreviewScreen(
                    imageLoader = imageLoader,
                    formArgs = formArgs,
                    bankName = screenState.bankName,
                    last4 = screenState.last4,
                    showCheckbox = usBankAccountFormArgs.showCheckbox,
                    instantDebits = usBankAccountFormArgs.instantDebits,
                    isProcessing = screenState.isProcessing,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
                    isTestMode = false,
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
                    imageLoader = imageLoader,
                    formArgs = formArgs,
                    bankName = screenState.paymentAccount.bankName,
                    last4 = screenState.paymentAccount.last4,
                    showCheckbox = usBankAccountFormArgs.showCheckbox,
                    instantDebits = usBankAccountFormArgs.instantDebits,
                    isProcessing = screenState.isProcessing,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
                    isTestMode = false,
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
                    imageLoader = imageLoader,
                    formArgs = formArgs,
                    bankName = screenState.bankName,
                    last4 = screenState.last4,
                    showCheckbox = usBankAccountFormArgs.showCheckbox,
                    instantDebits = usBankAccountFormArgs.instantDebits,
                    isProcessing = screenState.isProcessing,
                    isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
                    isTestMode = false,
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
    imageLoader: StripeImageLoader,
    formArgs: FormArguments,
    instantDebits: Boolean,
    isProcessing: Boolean,
    isPaymentFlow: Boolean,
    isTestMode: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
    featuredInstitutions: List<BankViewState>,
    onBankSelected: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BillingDetailsForm(
            imageLoader = imageLoader,
            instantDebits = instantDebits,
            formArgs = formArgs,
            isProcessing = isProcessing,
            isPaymentFlow = isPaymentFlow,
            isTestMode = isTestMode,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
            featuredInstitutions = featuredInstitutions,
            onBankSelected = onBankSelected,
        )
    }
}

@Composable
internal fun AccountPreviewScreen(
    imageLoader: StripeImageLoader,
    formArgs: FormArguments,
    bankName: String?,
    last4: String?,
    showCheckbox: Boolean,
    instantDebits: Boolean,
    isProcessing: Boolean,
    isPaymentFlow: Boolean,
    isTestMode: Boolean,
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
            imageLoader = imageLoader,
            formArgs = formArgs,
            instantDebits = instantDebits,
            isProcessing = isProcessing,
            isPaymentFlow = isPaymentFlow,
            isTestMode = isTestMode,
            nameController = nameController,
            emailController = emailController,
            phoneController = phoneController,
            addressController = addressController,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            sameAsShippingElement = sameAsShippingElement,
            featuredInstitutions = emptyList(),
            onBankSelected = {},
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
    imageLoader: StripeImageLoader,
    instantDebits: Boolean,
    formArgs: FormArguments,
    isProcessing: Boolean,
    isPaymentFlow: Boolean,
    isTestMode: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    phoneController: PhoneNumberController,
    addressController: AddressController,
    lastTextFieldIdentifier: IdentifierSpec?,
    sameAsShippingElement: SameAsShippingElement?,
    featuredInstitutions: List<BankViewState>,
    onBankSelected: (String) -> Unit,
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
        
        Spacer(modifier = Modifier.height(10.dp))

        EmbeddedBankPicker(
            imageLoader = imageLoader,
            featuredInstitutions = featuredInstitutions,
            onBankSelected = onBankSelected,
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Preview
@Composable
private fun EmbeddedBankPickerPreview() {
    StripeTheme {
        EmbeddedBankPicker(
            imageLoader = StripeImageLoader(LocalContext.current),
            featuredInstitutions = listOf(
                BankViewState(
                    Bank(
                        id = "1",
                        name = "Bank 1",
                        icon = "",
                        url = "https://b.stripecdn.com/connections-statics-srv/assets/BrandIcon--stripe-4x.png",
                        featuredOrder = 1,
                    )
                ),
                BankViewState(
                    Bank(
                        id = "2",
                        name = "Bank 2 with a really long name",
                        icon = "",
                        url = "https://b.stripecdn.com/connections-statics-srv/assets/BrandIcon--stripe-4x.png",
                        featuredOrder = 2,
                    )
                ),
                BankViewState(
                    Bank(
                        id = "3",
                        name = "Bank 3",
                        icon = "",
                        url = "https://b.stripecdn.com/connections-statics-srv/assets/BrandIcon--stripe-4x.png",
                        featuredOrder = 3,
                    )
                ),
                BankViewState(
                    Bank(
                        id = "4",
                        name = "Bank 4 with a really long name",
                        icon = "",
                        url = "https://b.stripecdn.com/connections-statics-srv/assets/BrandIcon--stripe-4x.png",
                        featuredOrder = 4,
                    )
                ),
            ),
            onBankSelected = {},
        )
    }
}

@Preview
@Composable
private fun EmbeddedBankPickerPreview_Loading() {
    StripeTheme {
        EmbeddedBankPicker(
            imageLoader = StripeImageLoader(LocalContext.current),
            featuredInstitutions = listOf(
                BankViewState(bank = null),
                BankViewState(bank = null),
                BankViewState(bank = null),
                BankViewState(bank = null),
                BankViewState(bank = null),
                BankViewState(bank = null),
            ),
            onBankSelected = {},
        )
    }
}

@Parcelize
internal data class BankViewState(
    val bank: Bank?,
) : Parcelable

@Composable
internal fun RowScope.EmbeddedBankPickerCard(
    bankViewState: BankViewState,
    imageLoader: StripeImageLoader,
    onBankSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.stripeColors.componentBorder,
        ),
        modifier = Modifier
            .weight(1f)
            .height(64.dp),
    ) {
        if (bankViewState.bank != null) {
            EmbeddedBankPickerCardContent(
                institution = bankViewState.bank,
                imageLoader = imageLoader,
                onBankSelected = onBankSelected,
            )
        } else {
            LoadingIndicator()
        }
    }
}

@Composable
private fun EmbeddedBankPickerCardContent(
    institution: Bank,
    imageLoader: StripeImageLoader,
    onBankSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onBankSelected(institution.id) }
            .padding(10.dp),
    ) {
        if (institution.icon != null) {
            StripeImage(
                url = institution.icon.orEmpty(),
                imageLoader = imageLoader,
                contentDescription = null,
                debugPainter = painterResource(R.drawable.stripe_ic_paymentsheet_bank),
                modifier = Modifier.height(48.dp),
                loadingContent = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.LightGray),
                    )
                }
            )
        } else {
            Text(text = institution.name)
        }
    }
}

@Composable
private fun EmbeddedBankPicker(
    imageLoader: StripeImageLoader,
    featuredInstitutions: List<BankViewState>,
    modifier: Modifier = Modifier,
    onBankSelected: (String) -> Unit,
) {
//    val numberOfRows = remember(featuredInstitutions) {
//        ceil(featuredInstitutions.size / 2f).roundToInt()
//    }
//
    val rows = remember(featuredInstitutions) {
        featuredInstitutions.chunked(2)
    }

    val spacing = remember { Arrangement.spacedBy(10.dp) }

    Column(
        verticalArrangement = spacing,
        modifier = modifier.fillMaxWidth(),
    ) {
        for (row in rows) {
            Row(horizontalArrangement = spacing) {
                for (item in row) {
                    EmbeddedBankPickerCard(
                        bankViewState = item,
                        imageLoader = imageLoader,
                        onBankSelected = onBankSelected,
                    )
                }
            }
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

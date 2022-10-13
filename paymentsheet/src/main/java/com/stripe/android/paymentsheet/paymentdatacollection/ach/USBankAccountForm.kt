package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import android.content.Context
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
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.NameAndEmailCollection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.VerifyWithMicrodeposits
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.elements.H6Text
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import com.stripe.android.ui.core.elements.SectionCard
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.elements.TextFieldSection
import com.stripe.android.ui.core.paymentsColors

@Composable
internal fun USBankAccountForm(
    args: FormFragmentArguments,
    sheetViewModel: BaseSheetViewModel<*>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    val viewModel = viewModel<USBankAccountFormViewModel>(
        factory = USBankAccountFormViewModel.Factory(
            applicationSupplier = { context.applicationContext as Application },
            argsSupplier = { createArgs(sheetViewModel, args) },
            owner = savedStateRegistryOwner,
        )
    )

    val launcher = CollectBankAccountLauncher.rememberLauncher(
        callback = viewModel::handleCollectBankAccountResult,
    )

    LaunchedEffect(viewModel) {
        viewModel.registerLauncher(launcher)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDestroy()
        }
    }

    val currentScreenState by viewModel.currentScreenState.collectAsState()
    val processing by viewModel.processing.collectAsState()

    SyncUSBankViewModelAndSheetViewModel(
        sheetViewModel = sheetViewModel,
        usBankViewModel = viewModel,
    )

    LaunchedEffect(currentScreenState) {
        handleScreenStateChanged(
            context = context,
            screenState = currentScreenState,
            sheetViewModel = sheetViewModel,
            viewModel = viewModel,
        )
    }

    Box(modifier) {
        when (val screenState = currentScreenState) {
            is NameAndEmailCollection -> {
                NameAndEmailCollectionScreen(
                    screenState = screenState,
                    processing = processing,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                )
            }
            is USBankAccountFormScreenState.MandateCollection -> {
                MandateCollectionScreen(
                    screenState = screenState,
                    processing = processing,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    saveForFutureUseElement = viewModel.saveForFutureUseElement,
                    showCheckbox = args.showCheckbox,
                    onBankAccountRemoved = viewModel::reset,
                )
            }
            is VerifyWithMicrodeposits -> {
                VerifyWithMicrodepositsScreen(
                    screenState = screenState,
                    processing = processing,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    saveForFutureUseElement = viewModel.saveForFutureUseElement,
                    showCheckbox = args.showCheckbox,
                    onBankAccountRemoved = viewModel::reset,
                )
            }
            is USBankAccountFormScreenState.SavedAccount -> {
                SavedAccountScreen(
                    screenState = screenState,
                    processing = processing,
                    nameController = viewModel.nameController,
                    emailController = viewModel.emailController,
                    saveForFutureUseElement = viewModel.saveForFutureUseElement,
                    showCheckbox = args.showCheckbox,
                    onBankAccountRemoved = viewModel::reset,
                )
            }
        }
    }
}

private fun handleScreenStateChanged(
    context: Context,
    screenState: USBankAccountFormScreenState,
    sheetViewModel: BaseSheetViewModel<*>,
    viewModel: USBankAccountFormViewModel,
) {
    sheetViewModel.onError(screenState.error)

    val completePayment = sheetViewModel is PaymentSheetViewModel
    val showProcessingWhenClicked = screenState is NameAndEmailCollection || completePayment
    val enabled = if (screenState is NameAndEmailCollection) {
        viewModel.requiredFields.value
    } else {
        true
    }

    sheetViewModel.updatePrimaryButton(
        text = screenState.primaryButtonText,
        onClick = { viewModel.handlePrimaryButtonClick(screenState) },
        enabled = enabled,
        shouldShowProcessingWhenClicked = showProcessingWhenClicked
    )

    sheetViewModel.updateMandateText(context, screenState.mandateText, viewModel)
}

private fun BaseSheetViewModel<*>.updatePrimaryButton(
    text: String?,
    onClick: () -> Unit,
    shouldShowProcessingWhenClicked: Boolean = true,
    enabled: Boolean = true,
    visible: Boolean = true
) {
    updatePrimaryButtonState(PrimaryButton.State.Ready)
    updatePrimaryButtonUIState(
        PrimaryButton.UIState(
            label = text,
            onClick = {
                if (shouldShowProcessingWhenClicked) {
                    updatePrimaryButtonState(
                        PrimaryButton.State.StartProcessing
                    )
                }
                onClick()
                updatePrimaryButtonUIState(
                    primaryButtonUIState.value?.copy(
                        onClick = null
                    )
                )
            },
            enabled = enabled,
            visible = visible
        )
    )
}

@Composable
private fun NameAndEmailCollectionScreen(
    screenState: NameAndEmailCollection,
    processing: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            name = screenState.name,
            email = screenState.email,
            processing = processing,
            nameController = nameController,
            emailController = emailController,
        )
    }
}

@Composable
private fun MandateCollectionScreen(
    screenState: USBankAccountFormScreenState.MandateCollection,
    processing: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    saveForFutureUseElement: SaveForFutureUseElement,
    showCheckbox: Boolean,
    onBankAccountRemoved: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            name = screenState.name,
            email = screenState.email,
            processing = processing,
            nameController = nameController,
            emailController = emailController,
        )

        AccountDetailsForm(
            processing,
            screenState.paymentAccount.institutionName,
            screenState.paymentAccount.last4,
            screenState.saveForFutureUsage,
            saveForFutureUseElement,
            showCheckbox,
            onBankAccountRemoved,
        )
    }
}

@Composable
private fun VerifyWithMicrodepositsScreen(
    screenState: VerifyWithMicrodeposits,
    processing: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    saveForFutureUseElement: SaveForFutureUseElement,
    showCheckbox: Boolean,
    onBankAccountRemoved: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            name = screenState.name,
            email = screenState.email,
            processing = processing,
            nameController = nameController,
            emailController = emailController,
        )

        AccountDetailsForm(
            processing,
            screenState.paymentAccount.bankName,
            screenState.paymentAccount.last4,
            screenState.saveForFutureUsage,
            saveForFutureUseElement,
            showCheckbox,
            onBankAccountRemoved,
        )
    }
}

@Composable
private fun SavedAccountScreen(
    screenState: USBankAccountFormScreenState.SavedAccount,
    processing: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
    saveForFutureUseElement: SaveForFutureUseElement,
    showCheckbox: Boolean,
    onBankAccountRemoved: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            name = screenState.name,
            email = screenState.email,
            processing = processing,
            nameController = nameController,
            emailController = emailController,
        )

        AccountDetailsForm(
            processing,
            screenState.bankName,
            screenState.last4,
            screenState.saveForFutureUsage,
            saveForFutureUseElement,
            showCheckbox,
            onBankAccountRemoved,
        )
    }
}

@Composable
private fun NameAndEmailForm(
    name: String,
    email: String?,
    processing: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController,
) {
    Column(Modifier.fillMaxWidth()) {
        H6Text(
            text = stringResource(R.string.stripe_paymentsheet_pay_with_bank_title),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
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
                imeAction = ImeAction.Done,
                enabled = !processing
            )
        }
    }
}

@Composable
private fun AccountDetailsForm(
    processing: Boolean,
    bankName: String?,
    last4: String?,
    saveForFutureUsage: Boolean,
    saveForFutureUseElement: SaveForFutureUseElement,
    showCheckbox: Boolean,
    onBankAccountRemoved: () -> Unit,
) {
    val openDialog = remember { mutableStateOf(false) }
    val bankIcon = TransformToBankIcon(bankName)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        H6Text(
            text = stringResource(R.string.title_bank_account),
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
                        color = MaterialTheme.paymentsColors.onComponent
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
        if (showCheckbox) {
            SaveForFutureUseElementUI(
                true,
                saveForFutureUseElement.apply {
                    this.controller.onValueChange(saveForFutureUsage)
                }
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
                id = R.string.bank_account_ending_in,
                last4
            ),
            confirmText = stringResource(
                id = R.string.remove
            ),
            dismissText = stringResource(
                id = R.string.cancel
            ),
            onConfirmListener = {
                openDialog.value = false
                onBankAccountRemoved()
            },
            onDismissListener = {
                openDialog.value = false
            }
        )
    }
}

private fun createArgs(
    sheetViewModel: BaseSheetViewModel<*>,
    formArgs: FormFragmentArguments,
): USBankAccountFormViewModel.Args {
    val savedPaymentMethod =
        sheetViewModel.newPaymentSelection as? PaymentSelection.New.USBankAccount

    val clientSecret = when (val intent = sheetViewModel.stripeIntent.value) {
        is PaymentIntent -> PaymentIntentClientSecret(intent.clientSecret!!)
        is SetupIntent -> SetupIntentClientSecret(intent.clientSecret!!)
        else -> null
    }

    return USBankAccountFormViewModel.Args(
        formArgs = formArgs,
        isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
        clientSecret = clientSecret,
        savedPaymentMethod = savedPaymentMethod,
        shippingDetails = sheetViewModel.config?.shippingDetails,
        onConfirmStripeIntent = { params ->
            (sheetViewModel as? PaymentSheetViewModel)?.confirmStripeIntent(params)
        },
        onUpdateSelectionAndFinish = { paymentSelection ->
            sheetViewModel.updateSelection(paymentSelection)
            sheetViewModel.onFinish()
        }
    )
}

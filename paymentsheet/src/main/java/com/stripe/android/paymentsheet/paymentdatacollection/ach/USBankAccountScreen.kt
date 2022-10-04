package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.H6Text
import com.stripe.android.ui.core.elements.SectionCard
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.elements.TextFieldSection
import com.stripe.android.ui.core.paymentsColors

@Composable
internal fun USBankAccountScreen(
    currentScreenState: USBankAccountFormScreenState,
    enabled: Boolean,
    saveForFutureUseUI: @Composable ColumnScope.(Boolean) -> Unit,
    nameController: TextFieldController,
    emailController: TextFieldController,
    onConfirm: () -> Unit
) {
    when (currentScreenState) {
        is USBankAccountFormScreenState.NameAndEmailCollection -> {
            NameAndEmailCollectionScreen(
                currentScreenState,
                enabled,
                nameController,
                emailController
            )
        }
        is USBankAccountFormScreenState.MandateCollection -> {
            MandateCollectionScreen(
                currentScreenState,
                enabled,
                saveForFutureUseUI,
                nameController,
                emailController,
                onConfirm
            )
        }
        is USBankAccountFormScreenState.VerifyWithMicrodeposits -> {
            VerifyWithMicrodepositsScreen(
                currentScreenState,
                enabled,
                saveForFutureUseUI,
                nameController,
                emailController,
                onConfirm
            )
        }
        is USBankAccountFormScreenState.SavedAccount -> {
            SavedAccountScreen(
                currentScreenState,
                enabled,
                saveForFutureUseUI,
                nameController,
                emailController,
                onConfirm
            )
        }
    }
}

@Composable
private fun NameAndEmailCollectionScreen(
    screenState: USBankAccountFormScreenState.NameAndEmailCollection,
    enabled: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            screenState.name,
            screenState.email,
            enabled,
            nameController,
            emailController
        )
    }
}

@Composable
private fun MandateCollectionScreen(
    screenState: USBankAccountFormScreenState.MandateCollection,
    enabled: Boolean,
    saveForFutureUseUI: @Composable ColumnScope.(Boolean) -> Unit,
    nameController: TextFieldController,
    emailController: TextFieldController,
    onConfirm: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            screenState.name,
            screenState.email,
            enabled,
            nameController,
            emailController
        )
        AccountDetailsForm(
            screenState.paymentAccount.institutionName,
            screenState.paymentAccount.last4,
            screenState.saveForFutureUsage,
            enabled,
            saveForFutureUseUI,
            onConfirm
        )
    }
}

@Composable
private fun VerifyWithMicrodepositsScreen(
    screenState: USBankAccountFormScreenState.VerifyWithMicrodeposits,
    enabled: Boolean,
    saveForFutureUseUI: @Composable ColumnScope.(Boolean) -> Unit,
    nameController: TextFieldController,
    emailController: TextFieldController,
    onConfirm: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            screenState.name,
            screenState.email,
            enabled,
            nameController,
            emailController
        )
        AccountDetailsForm(
            screenState.paymentAccount.bankName,
            screenState.paymentAccount.last4,
            screenState.saveForFutureUsage,
            enabled,
            saveForFutureUseUI,
            onConfirm
        )
    }
}

@Composable
private fun SavedAccountScreen(
    screenState: USBankAccountFormScreenState.SavedAccount,
    enabled: Boolean,
    saveForFutureUseUI: @Composable ColumnScope.(Boolean) -> Unit,
    nameController: TextFieldController,
    emailController: TextFieldController,
    onConfirm: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        NameAndEmailForm(
            screenState.name,
            screenState.email,
            enabled,
            nameController,
            emailController
        )
        AccountDetailsForm(
            screenState.bankName,
            screenState.last4,
            screenState.saveForFutureUsage,
            enabled,
            saveForFutureUseUI,
            onConfirm
        )
    }
}

@Composable
private fun NameAndEmailForm(
    name: String,
    email: String?,
    enabled: Boolean,
    nameController: TextFieldController,
    emailController: TextFieldController
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
                enabled = enabled
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
                enabled = enabled
            )
        }
    }
}

@Composable
private fun AccountDetailsForm(
    bankName: String?,
    last4: String?,
    saveForFutureUsage: Boolean,
    enabled: Boolean,
    saveForFutureUseUI: @Composable ColumnScope.(Boolean) -> Unit,
    onConfirm: () -> Unit
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
                        modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
                        color = MaterialTheme.paymentsColors.onComponent
                    )
                }
                Image(
                    painter = painterResource(R.drawable.stripe_ic_clear),
                    contentDescription = null,
                    modifier = Modifier
                        .height(20.dp)
                        .width(20.dp)
                        .alpha(if (enabled) 1f else 0.5f)
                        .clickable(enabled = enabled) {
                            openDialog.value = true
                        }
                )
            }
        }
        saveForFutureUseUI(saveForFutureUsage)
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
                onConfirm()
            },
            onDismissListener = {
                openDialog.value = false
            }
        )
    }
}
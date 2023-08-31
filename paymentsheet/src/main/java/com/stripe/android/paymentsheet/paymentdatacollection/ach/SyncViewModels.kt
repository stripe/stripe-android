package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import kotlinx.coroutines.flow.filterNot

@Composable
internal fun SyncViewModels(
    viewModel: USBankAccountFormViewModel,
    handleConfirmUSBankAccount: (PaymentSelection.New.USBankAccount) -> Unit,
    updatePrimaryButton: ((PrimaryButton.UIState?) -> PrimaryButton.UIState?) -> Unit,
    updateMandateText: (String) -> Unit
) {
    val context = LocalContext.current
    val screenState = viewModel.currentScreenState.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.result.collect { result ->
            handleConfirmUSBankAccount(result)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.requiredFields.collect { hasRequiredFields ->
            updatePrimaryButton {
                it?.copy(enabled = hasRequiredFields)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveForFutureUse.filterNot {
            screenState is USBankAccountFormScreenState.BillingDetailsCollection
        }.collect { saved ->
            val merchantName = viewModel.formattedMerchantName()
            val mandateText = ACHText.getContinueMandateText(
                context = context,
                merchantName = merchantName,
                isSaveForFutureUseSelected = saved,
            )

            val microdepositsText = if (screenState is USBankAccountFormScreenState.VerifyWithMicrodeposits) {
                context.getString(R.string.stripe_paymentsheet_microdeposit, merchantName)
            } else {
                ""
            }

            updateMandateText(
                """
                    $microdepositsText
                        
                    $mandateText
                """.trimIndent(),
            )
        }
    }
}

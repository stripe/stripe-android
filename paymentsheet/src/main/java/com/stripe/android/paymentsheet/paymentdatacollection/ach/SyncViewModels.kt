package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.flow.filterNot

@Composable
internal fun SyncViewModels(
    viewModel: USBankAccountFormViewModel,
    sheetViewModel: BaseSheetViewModel,
    onMandateTextChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val screenState by viewModel.currentScreenState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.result.collect { result ->
            sheetViewModel.handleConfirmUSBankAccount(result)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.requiredFields.collect { hasRequiredFields ->
            sheetViewModel.updateCustomPrimaryButtonUiState {
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

            onMandateTextChanged(
                """
                    $microdepositsText
                        
                    $mandateText
                """.trimIndent(),
            )
        }
    }
}

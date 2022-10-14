package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.VerifyWithMicrodeposits
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

@Composable
internal fun SyncUSBankViewModelAndSheetViewModel(
    sheetViewModel: BaseSheetViewModel<*>,
    usBankViewModel: USBankAccountFormViewModel,
) {
    val primaryButtonState by sheetViewModel.primaryButtonState.observeAsState()
    LaunchedEffect(primaryButtonState) {
        // When the primary button state is StartProcessing or FinishProcessing
        // we should disable the inputs of this form. StartProcessing shows the loading
        // spinner, FinishProcessing shows the checkmark animation
        usBankViewModel.setProcessing(
            primaryButtonState is PrimaryButton.State.StartProcessing ||
                primaryButtonState is PrimaryButton.State.FinishProcessing
        )
    }

    val requiredFields by usBankViewModel.requiredFields.collectAsState()
    LaunchedEffect(requiredFields) {
        sheetViewModel.updatePrimaryButtonUIState(
            sheetViewModel.primaryButtonUIState.value?.copy(
                enabled = requiredFields,
            )
        )
    }

    val saveForFutureUse by usBankViewModel.saveForFutureUse.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(saveForFutureUse) {
        val text = if (saveForFutureUse) {
            context.getString(
                R.string.stripe_paymentsheet_ach_save_mandate,
                usBankViewModel.formattedMerchantName()
            )
        } else {
            ACHText.getContinueMandateText(context)
        }

        sheetViewModel.updateMandateText(
            context = context,
            mandateText = text,
            viewModel = usBankViewModel,
        )
    }
}

internal fun BaseSheetViewModel<*>.updateMandateText(
    context: Context,
    mandateText: String?,
    viewModel: USBankAccountFormViewModel,
) {
    val microdepositsText = if (viewModel.currentScreenState.value is VerifyWithMicrodeposits) {
        context.getString(
            R.string.stripe_paymentsheet_microdeposit,
            viewModel.formattedMerchantName()
        )
    } else {
        ""
    }

    val updatedText = mandateText?.let {
        """
            $microdepositsText
                
            $mandateText
        """.trimIndent()
    }

    updateBelowButtonText(updatedText)
}

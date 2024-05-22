package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.uicore.utils.collectAsStateSafely
import kotlinx.coroutines.flow.filterNot

@Composable
internal fun USBankAccountEmitters(
    viewModel: USBankAccountFormViewModel,
    usBankAccountFormArgs: USBankAccountFormArguments,
) {
    val context = LocalContext.current
    val screenState by viewModel.currentScreenState.collectAsStateSafely()
    val hasRequiredFields by viewModel.requiredFields.collectAsStateSafely()
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current

    LaunchedEffect(Unit) {
        viewModel.result.collect { result ->
            result?.let {
                usBankAccountFormArgs.onConfirmUSBankAccount(result)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.collectBankAccountResult.collect { result ->
            result?.let {
                usBankAccountFormArgs.onCollectBankAccountResult?.invoke(result)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.requiredFields.collect { hasRequiredFields ->
            usBankAccountFormArgs.onUpdatePrimaryButtonUIState {
                it?.copy(enabled = hasRequiredFields)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveForFutureUse.filterNot {
            screenState is USBankAccountFormScreenState.BillingDetailsCollection
        }.collect { saved ->
            val merchantName = viewModel.formattedMerchantName()
            val mandateText = USBankAccountTextBuilder.getContinueMandateText(
                context = context,
                merchantName = merchantName,
                isSaveForFutureUseSelected = saved,
                isInstantDebits = usBankAccountFormArgs.instantDebits,
                isSetupFlow = !usBankAccountFormArgs.isPaymentFlow,
            )
            usBankAccountFormArgs.updateMandateText(
                context = context,
                screenState = screenState,
                mandateText = mandateText,
                merchantName = merchantName
            )
        }
    }

    LaunchedEffect(screenState, hasRequiredFields) {
        usBankAccountFormArgs.handleScreenStateChanged(
            context = context,
            screenState = screenState,
            enabled = hasRequiredFields && !screenState.isProcessing,
            merchantName = viewModel.formattedMerchantName(),
            onPrimaryButtonClick = viewModel::handlePrimaryButtonClick,
        )
    }

    DisposableEffect(Unit) {
        viewModel.register(activityResultRegistryOwner!!)

        onDispose {
            usBankAccountFormArgs.onUpdatePrimaryButtonUIState { null }
            viewModel.onDestroy()
        }
    }
}

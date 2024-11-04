package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.filterNotNull

@Composable
internal fun USBankAccountEmitters(
    viewModel: USBankAccountFormViewModel,
    usBankAccountFormArgs: USBankAccountFormArguments,
) {
    val screenState by viewModel.currentScreenState.collectAsState()
    val hasRequiredFields by viewModel.requiredFields.collectAsState()
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current

    LaunchedEffect(Unit) {
        viewModel.result.filterNotNull().collect { result ->
            usBankAccountFormArgs.onBankAccountLinked(result)
        }
    }

    usBankAccountFormArgs.onCollectBankAccountResult?.let { collectListener ->
        LaunchedEffect(Unit) {
            viewModel.collectBankAccountResult.filterNotNull().collect { result ->
                collectListener(result)
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

    LaunchedEffect(screenState, hasRequiredFields) {
        usBankAccountFormArgs.handleScreenStateChanged(
            screenState = screenState,
            enabled = hasRequiredFields && !screenState.isProcessing,
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

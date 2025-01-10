package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun PartnerAuthScreen(inModal: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isReady = remember { mutableStateOf(false) }

    // Track the lifecycle to update the readiness state
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> isReady.value = true
                Lifecycle.Event.ON_DESTROY -> isReady.value = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Only do state collection and UI updates when ready
    if (!inModal || isReady.value) {
        val viewModel: PartnerAuthViewModel = paneViewModel {
            PartnerAuthViewModel.factory(
                parentComponent = it,
                args = PartnerAuthViewModel.Args(inModal, Pane.PARTNER_AUTH)
            )
        }
        val state: State<SharedPartnerAuthState> = viewModel.stateFlow.collectAsState()

        SharedPartnerAuth(
            inModal = inModal,
            state = state.value,
            onContinueClick = viewModel::onLaunchAuthClick,
            onCancelClick = viewModel::onCancelClick,
            onClickableTextClick = viewModel::onClickableTextClick,
            onWebAuthFlowFinished = viewModel::onWebAuthFlowFinished,
            onViewEffectLaunched = viewModel::onViewEffectLaunched
        )
    }
}
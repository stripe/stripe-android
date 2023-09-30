@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.SharedPartnerAuthContent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import kotlinx.coroutines.launch

@Composable
internal fun PartnerAuthScreen() {
    // activity view model
    val parentViewModel = parentViewModel()
    val webAuthFlow = parentViewModel.collectAsState { it.webAuthFlow }
    val uriHandler = LocalUriHandler.current

    // step view model
    val viewModel: PartnerAuthViewModel = mavericksViewModel()

    val state: State<PartnerAuthState> = viewModel.collectAsState()

    val scope = rememberCoroutineScope()

    val bottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenBottomSheet -> bottomSheetState.show()
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
                is OpenPartnerAuth -> {
                    parentViewModel.openPartnerAuthFlowInBrowser(viewEffect.url)
                    viewModel.onViewEffectLaunched()
                }
            }
        }
    }

    LaunchedEffect(webAuthFlow.value) {
        viewModel.onWebAuthFlowFinished(webAuthFlow.value)
    }

    SharedPartnerAuthContent(
        state = state.value,
        onContinueClick = viewModel::onLaunchAuthClick,
        onSelectAnotherBank = viewModel::onSelectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManuallyClick,
        modalBottomSheetState = bottomSheetState,
        onClickableTextClick = viewModel::onClickableTextClick,
        onConfirmModalClick = { scope.launch { bottomSheetState.hide() } },
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.PARTNER_AUTH) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Preview(
    group = "Partner Auth Pane"
)
@Composable
internal fun PartnerAuthPreview(
    @PreviewParameter(PartnerAuthPreviewParameterProvider::class)
    state: PartnerAuthState
) {
    FinancialConnectionsPreview {
        SharedPartnerAuthContent(
            state = state,
            onContinueClick = {},
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onCloseClick = {},
            modalBottomSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden
            ),
            onClickableTextClick = {},
            onCloseFromErrorClick = {},
            onConfirmModalClick = {}
        )
    }
}

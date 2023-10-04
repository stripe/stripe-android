@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.SharedPartnerAuthContent
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview

@Composable
internal fun PartnerAuthScreen() {
    val viewModel: PartnerAuthViewModel = mavericksViewModel()
    val state: State<SharedPartnerAuthState> = viewModel.collectAsState()

    SharedPartnerAuthContent(
        state = state.value,
        onContinueClick = viewModel::onLaunchAuthClick,
        onSelectAnotherBank = viewModel::onSelectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManuallyClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onWebAuthFlowFinished = viewModel::onWebAuthFlowFinished,
        onViewEffectLaunched = viewModel::onViewEffectLaunched
    )
}

@Preview(
    group = "Partner Auth Pane"
)
@Composable
internal fun PartnerAuthPreview(
    @PreviewParameter(PartnerAuthPreviewParameterProvider::class)
    state: SharedPartnerAuthState
) {
    FinancialConnectionsPreview {
        SharedPartnerAuthContent(
            state = state,
            onContinueClick = {},
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onClickableTextClick = {},
            onWebAuthFlowFinished = {},
            onViewEffectLaunched = {}
        )
    }
}

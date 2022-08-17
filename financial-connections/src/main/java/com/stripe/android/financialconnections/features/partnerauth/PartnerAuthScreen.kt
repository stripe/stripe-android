package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.institutionpicker.LoadingContent
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun PartnerAuthScreen() {
    // activity view model
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val webAuthFlow = activityViewModel.collectAsState { it.webAuthFlow }

    // step view model
    val viewModel: PartnerAuthViewModel = mavericksViewModel()
    val state: State<PartnerAuthState> = viewModel.collectAsState()

    LaunchedEffect(state.value.url) {
        val url = state.value.url
        if (url != null) activityViewModel.openPartnerAuthFlowInBrowser(url)
    }
    LaunchedEffect(webAuthFlow.value) {
        viewModel.onWebAuthFlowFinished(webAuthFlow.value)
    }
    PartnerAuthScreenContent(state.value, viewModel::onLaunchAuthClick)
}

@Composable
private fun PartnerAuthScreenContent(
    state: PartnerAuthState,
    onContinueClick: () -> Unit
) {
    FinancialConnectionsScaffold {
        when (state.authenticationStatus) {
            is Uninitialized -> PrePaneContent(
                state.institutionName, state.partner, onContinueClick)
            is Loading, is Success -> LoadingContent(
                titleResId = R.string.stripe_picker_loading_title,
                contentResId = R.string.stripe_picker_loading_desc
            )
            is Fail -> {
                // TODO@carlosmuvi translate error type to specific error screen.
                UnclassifiedErrorContent()
            }
        }
    }
}

@Composable
private fun PrePaneContent(
    institutionName: String,
    partner: PartnerAuthState.Partner,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_prepane_title, institutionName),
            style = FinancialConnectionsTheme.typography.subtitle
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_prepane_desc, institutionName),
            style = FinancialConnectionsTheme.typography.body
        )
        Spacer(modifier = Modifier.weight(1f))
        Callout(partner = partner)
        Spacer(modifier = Modifier.size(16.dp))
        FinancialConnectionsButton(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.stripe_prepane_continue),
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
                Icon(
                    painterResource(id = R.drawable.stripe_ic_external),
                    modifier = Modifier.align(Alignment.CenterEnd),
                    tint = FinancialConnectionsTheme.colors.textWhite,
                    contentDescription = stringResource(R.string.stripe_prepane_continue)
                )
            }
        }
    }
}

@Composable
private fun Callout(
    partner: PartnerAuthState.Partner
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color = FinancialConnectionsTheme.colors.backgroundContainer)
            .padding(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.size(16.dp))
        AnnotatedText(
            TextResource.StringId(
                R.string.stripe_prepane_partner_callout,
                listOf(stringResource(id = partner.toStringResId()))
            ),
            textStyle = FinancialConnectionsTheme.typography.captionTight.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
            ),
            clickableStyle = FinancialConnectionsTheme.typography.captionTightEmphasized
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textBrand),
            onClickableTextClick = {}
        )
    }
}

private fun PartnerAuthState.Partner.toStringResId(): Int = when (this) {
    PartnerAuthState.Partner.FINICITY -> R.string.stripe_partner_finicity
    PartnerAuthState.Partner.MX -> R.string.stripe_partner_mx
    PartnerAuthState.Partner.TESTMODE -> R.string.stripe_partner_testmode
    PartnerAuthState.Partner.TRUELAYER -> R.string.stripe_partner_truelayer
    PartnerAuthState.Partner.WELLS_FARGO -> R.string.stripe_partner_wellsfargo
}

@Composable
@Preview
internal fun PrepaneContentPreview() {
    FinancialConnectionsTheme {
        PartnerAuthScreenContent(
            state = PartnerAuthState(
                institutionName = "Random bank",
                url = null,
                authenticationStatus = Uninitialized,
                partner = PartnerAuthState.Partner.FINICITY
            ),
            onContinueClick = {}
        )
    }
}

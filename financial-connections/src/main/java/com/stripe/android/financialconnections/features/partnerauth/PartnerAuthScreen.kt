package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.features.common.InstitutionPlaceholder
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnknownErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.PartnerCallout
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.PartnerAuthViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession.Flow
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun PartnerAuthScreen() {
    // activity view model
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val parentViewModel = parentViewModel()
    val webAuthFlow = activityViewModel.collectAsState { it.webAuthFlow }

    // step view model
    val viewModel: PartnerAuthViewModel = mavericksViewModel()
    val state: State<PartnerAuthState> = viewModel.collectAsState()

    ObserveViewEffect(state, activityViewModel, viewModel)
    LaunchedEffect(webAuthFlow.value) {
        viewModel.onWebAuthFlowFinished(webAuthFlow.value)
    }
    PartnerAuthScreenContent(
        state = state.value,
        onContinueClick = viewModel::onLaunchAuthClick,
        onSelectAnotherBank = viewModel::onSelectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManuallyClick,
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(NextPane.PARTNER_AUTH) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ObserveViewEffect(
    state: State<PartnerAuthState>,
    activityViewModel: FinancialConnectionsSheetNativeViewModel,
    viewModel: PartnerAuthViewModel
) {
    LaunchedEffect(state.value.viewEffect) {
        when (val viewEffect = state.value.viewEffect) {
            null -> Unit
            is OpenPartnerAuth -> {
                activityViewModel.openPartnerAuthFlowInBrowser(viewEffect.url)
                viewModel.onViewEffectLaunched()
            }

        }
    }
}

@Composable
private fun PartnerAuthScreenContent(
    state: PartnerAuthState,
    onContinueClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = state.canNavigateBack,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> LoadingContent(
                stringResource(id = R.string.stripe_picker_loading_title),
                stringResource(id = R.string.stripe_picker_loading_desc)
            )

            is Fail -> ErrorContent(
                error = payload.error,
                onSelectAnotherBank = onSelectAnotherBank,
                onEnterDetailsManually = onEnterDetailsManually,
                onCloseFromErrorClick = onCloseFromErrorClick
            )

            is Success -> LoadedContent(
                authenticationStatus = state.authenticationStatus,
                payload = payload(),
                onContinueClick = onContinueClick,
                onSelectAnotherBank = onSelectAnotherBank
            )
        }
    }
}

@Composable
fun ErrorContent(
    error: Throwable,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    when (error) {
        is InstitutionPlannedDowntimeError -> InstitutionPlannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        is InstitutionUnplannedDowntimeError -> InstitutionUnplannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        else -> UnclassifiedErrorContent(error, onCloseFromErrorClick)
    }
}

@Composable
private fun LoadedContent(
    authenticationStatus: Async<String>,
    payload: PartnerAuthState.Payload,
    onContinueClick: () -> Unit,
    onSelectAnotherBank: () -> Unit
) {
    when (authenticationStatus) {
        is Uninitialized -> when (payload.showPrepane) {
            true -> PrePaneContent(
                institution = payload.institution,
                flow = payload.flow,
                showPartnerDisclosure = payload.showPartnerDisclosure,
                onContinueClick = onContinueClick
            )

            false -> LoadingContent(
                stringResource(id = R.string.stripe_picker_loading_title),
                stringResource(id = R.string.stripe_picker_loading_desc)
            )
        }

        is Loading, is Success -> LoadingContent(
            stringResource(id = R.string.stripe_picker_loading_title),
            stringResource(id = R.string.stripe_picker_loading_desc)
        )

        is Fail -> {
            // TODO@carlosmuvi translate error type to specific error screen.
            InstitutionUnknownErrorContent(onSelectAnotherBank)
        }
    }
}

@Composable
private fun PrePaneContent(
    institution: FinancialConnectionsInstitution,
    flow: Flow?,
    showPartnerDisclosure: Boolean,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(
                top = 24.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
    ) {
        val modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
        StripeImage(
            url = institution.icon?.default ?: "",
            contentDescription = null,
            imageLoader = LocalImageLoader.current,
            errorContent = { InstitutionPlaceholder(modifier) },
            modifier = modifier
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_prepane_title, institution.name),
            style = FinancialConnectionsTheme.typography.subtitle
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_prepane_desc, institution.name),
            style = FinancialConnectionsTheme.typography.body
        )
        Spacer(modifier = Modifier.weight(1f))
        if (flow != null && showPartnerDisclosure) PartnerCallout(flow)
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
@Preview
internal fun PrepaneContentPreview() {
    FinancialConnectionsTheme {
        PartnerAuthScreenContent(
            state = PartnerAuthState(
                payload = Success(
                    PartnerAuthState.Payload(
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "name",
                            url = "url",
                            featured = true,
                            icon = null,
                            logo = null,
                            featuredOrder = null,
                            mobileHandoffCapable = false
                        ),
                        flow = Flow.FINICITY_CONNECT_V2_OAUTH,
                        showPartnerDisclosure = true,
                        showPrepane = true
                    )
                ),
                authenticationStatus = Uninitialized,
                viewEffect = null
            ),
            onContinueClick = {},
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onCloseClick = {}
        ) {}
    }
}

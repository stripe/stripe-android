@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.AccessibleDataCallout
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun LinkAccountPickerScreen() {
    val viewModel: LinkAccountPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    LinkAccountPickerContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.NETWORKING_LINK_SIGNUP_PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onLearnMoreAboutDataAccessClick = { viewModel.onLearnMoreAboutDataAccessClick() }
    )
}

@Composable
private fun LinkAccountPickerContent(
    state: LinkAccountPickerState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onLearnMoreAboutDataAccessClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> LoadingContent()
            is Success -> LinkAccountPickerLoaded(
                scrollState = scrollState,
                payload = payload(),
                onLearnMoreAboutDataAccessClick = onLearnMoreAboutDataAccessClick
            )

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun LinkAccountPickerLoaded(
    scrollState: ScrollState,
    payload: Payload,
    onLearnMoreAboutDataAccessClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = 0.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Title()
        Spacer(modifier = Modifier.size(24.dp))
        payload.accounts.forEach {
            Text(text = it.name)
        }
        Text(text = "new bank account")
        AccessibleDataCallout(
            model = payload.accessibleDataCalloutModel,
            onLearnMoreClick = onLearnMoreAboutDataAccessClick
        )
        Spacer(modifier = Modifier.size(24.dp))
        FinancialConnectionsButton(onClick = { /*TODO*/ }) {}
    }
}

@Composable
private fun Title() {
    AnnotatedText(
        text = TextResource.Text(
            stringResource(R.string.stripe_networking_verification_title)
        ),
        defaultStyle = FinancialConnectionsTheme.typography.subtitle,
        annotationStyles = emptyMap(),
        onClickableTextClick = {},
    )
}

@Composable
@Preview(group = "LinkAccountPicker Pane", name = "Cannonical")
internal fun LinkAccountPickerScreenPreview() {
    FinancialConnectionsPreview {
        LinkAccountPickerContent(
            state = LinkAccountPickerState(
                payload = Success(
                    Payload(
                        accounts = listOf(),
                        accessibleDataCalloutModel = AccessibleDataCalloutModel(
                            businessName = "My business",
                            permissions = listOf(
                                FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
                                FinancialConnectionsAccount.Permissions.BALANCES,
                                FinancialConnectionsAccount.Permissions.OWNERSHIP,
                                FinancialConnectionsAccount.Permissions.TRANSACTIONS
                            ),
                            isStripeDirect = true,
                            dataPolicyUrl = ""
                        )
                    )
                )
            ),
            onCloseClick = {},
            onCloseFromErrorClick = {},
            onLearnMoreAboutDataAccessClick = {}
        )
    }
}

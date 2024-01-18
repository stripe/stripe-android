package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.ListItem
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Payload
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupViewModel.Companion.PANE
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.financialconnections.ui.theme.StripeThemeForConnections
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection

@Composable
internal fun NetworkingLinkSignupScreen() {
    val viewModel: NetworkingLinkSignupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    val uriHandler = LocalUriHandler.current

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
            }
            viewModel.onViewEffectLaunched()
        }
    }

    NetworkingLinkSignupContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onSaveToLink = viewModel::onSaveAccount,
        onSkipClick = viewModel::onSkipClick
    )
}

@Composable
private fun NetworkingLinkSignupContent(
    state: NetworkingLinkSignupState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSaveToLink: () -> Unit,
    onSkipClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick,
                elevation = scrollState.elevation
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> FullScreenGenericLoading()
            is Success -> NetworkingLinkSignupLoaded(
                validForm = state.valid(),
                payload = payload(),
                lookupAccountSync = state.lookupAccount,
                saveAccountToLinkSync = state.saveAccountToLink,
                showFullForm = state.showFullForm,
                onSaveToLink = onSaveToLink,
                onClickableTextClick = onClickableTextClick,
                onSkipClick = onSkipClick
            )

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NetworkingLinkSignupLoaded(
    validForm: Boolean,
    payload: Payload,
    saveAccountToLinkSync: Async<FinancialConnectionsSessionManifest>,
    lookupAccountSync: Async<ConsumerSessionLookup>,
    showFullForm: Boolean,
    onClickableTextClick: (String) -> Unit,
    onSaveToLink: () -> Unit,
    onSkipClick: () -> Unit
) {
    Layout(
        body = {
            item {
                Title(payload.content.title)
                Spacer(modifier = Modifier.size(24.dp))
            }
            items(payload.content.body.bullets) {
                ListItem(
                    bullet = BulletUI.from(it),
                    onClickableTextClick = onClickableTextClick
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
            item {
                EmailSection(
                    showFullForm = showFullForm,
                    loading = lookupAccountSync is Loading,
                    emailController = payload.emailController,
                    enabled = true,
                )
            }

            if (showFullForm) {
                item { PhoneNumberSection(payload = payload) }
            }
        },
        footer = {
            Column {
                if (showFullForm) {
                    SaveToLinkCta(
                        text = payload.content.cta,
                        aboveCta = payload.content.aboveCta,
                        onClickableTextClick = onClickableTextClick,
                        saveAccountToLinkSync = saveAccountToLinkSync,
                        validForm = validForm,
                        onSaveToLink = onSaveToLink
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                SkipCta(payload.content.skipCta, onSkipClick)
            }
        }
    )
}

@Composable
private fun SkipCta(text: String, onSkipClick: () -> Unit) {
    FinancialConnectionsButton(
        type = FinancialConnectionsButton.Type.Secondary,
        onClick = onSkipClick,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(text = text)
    }
}

@Composable
private fun SaveToLinkCta(
    aboveCta: String,
    text: String,
    onClickableTextClick: (String) -> Unit,
    saveAccountToLinkSync: Async<FinancialConnectionsSessionManifest>,
    validForm: Boolean,
    onSaveToLink: () -> Unit
) {
    Column {
        AnnotatedText(
            modifier = Modifier.fillMaxWidth(),
            text = TextResource.Text(fromHtml(aboveCta)),
            onClickableTextClick = onClickableTextClick,
            defaultStyle = v3Typography.labelSmall.copy(
                textAlign = TextAlign.Center,
                color = v3Colors.textDefault
            )
        )
        Spacer(modifier = Modifier.size(8.dp))
        FinancialConnectionsButton(
            loading = saveAccountToLinkSync is Loading,
            enabled = validForm,
            type = FinancialConnectionsButton.Type.Primary,
            onClick = onSaveToLink,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = text)
        }
    }
}

@Composable
private fun PhoneNumberSection(
    payload: Payload
) {
    Column {
        StripeThemeForConnections {
            PhoneNumberCollectionSection(
                requestFocusWhenShown = payload.phoneController.initialPhoneNumber.isEmpty(),
                phoneNumberController = payload.phoneController,
                imeAction = ImeAction.Default,
                enabled = true,
            )
        }
    }
}

@Composable
private fun Title(title: String) {
    AnnotatedText(
        text = TextResource.Text(fromHtml(title)),
        defaultStyle = v3Typography.headingLarge,
        onClickableTextClick = {},
    )
}

@Composable
internal fun EmailSection(
    enabled: Boolean,
    emailController: TextFieldController,
    showFullForm: Boolean,
    loading: Boolean
) {
    StripeThemeForConnections {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextFieldSection(
                textFieldController = emailController,
                imeAction = if (showFullForm) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                },
                enabled = enabled
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(
                            start = 0.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = 8.dp
                        ),
                    color = v3Colors.iconBrand,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
@Preview(group = "NetworkingLinkSignup Pane")
internal fun NetworkingLinkSignupScreenPreview(
    @PreviewParameter(NetworkingLinkSignupPreviewParameterProvider::class)
    state: NetworkingLinkSignupState
) {
    FinancialConnectionsPreview {
        NetworkingLinkSignupContent(
            state = state,
            onCloseClick = {},
            onSaveToLink = {},
            onClickableTextClick = {},
            onCloseFromErrorClick = {},
            onSkipClick = {}
        )
    }
}

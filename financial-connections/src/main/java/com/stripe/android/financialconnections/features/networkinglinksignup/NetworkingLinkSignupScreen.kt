package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.stripe.android.financialconnections.features.common.LegalDetailsBottomSheetContent
import com.stripe.android.financialconnections.features.common.ListItem
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Payload
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.ViewEffect
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupViewModel.Companion.PANE
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsModalBottomSheetLayout
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
import kotlinx.coroutines.launch

@Composable
internal fun NetworkingLinkSignupScreen() {
    val viewModel: NetworkingLinkSignupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    val uriHandler = LocalUriHandler.current
    val bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
                is ViewEffect.OpenBottomSheet -> bottomSheetState.show()
            }
            viewModel.onViewEffectLaunched()
        }
    }

    NetworkingLinkSignupContent(
        state = state.value,
        bottomSheetState = bottomSheetState,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onSaveToLink = viewModel::onSaveAccount,
        onSkipClick = viewModel::onSkipClick
    )
}

@Composable
private fun NetworkingLinkSignupContent(
    bottomSheetState: ModalBottomSheetState,
    state: NetworkingLinkSignupState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSaveToLink: () -> Unit,
    onSkipClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    FinancialConnectionsModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            when (val legalDetails = state.payload()?.content?.legalDetailsNotice) {
                null -> {}
                else -> LegalDetailsBottomSheetContent(
                    legalDetails = legalDetails,
                    onConfirmModalClick = { coroutineScope.launch { bottomSheetState.hide() } },
                    onClickableTextClick = onClickableTextClick
                )
            }
        },
        content = {
            NetworkingLinkSignupMainContent(
                onCloseClick = onCloseClick,
                state = state,
                onSaveToLink = onSaveToLink,
                onClickableTextClick = onClickableTextClick,
                onSkipClick = onSkipClick,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    )
}

@Composable
private fun NetworkingLinkSignupMainContent(
    onCloseClick: () -> Unit,
    state: NetworkingLinkSignupState,
    onSaveToLink: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSkipClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    val lazyListState = rememberLazyListState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                elevation = lazyListState.elevation,
                showBack = false,
                onCloseClick = onCloseClick,
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> FullScreenGenericLoading()
            is Success -> NetworkingLinkSignupLoaded(
                lazyListState = lazyListState,
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

@Composable
private fun NetworkingLinkSignupLoaded(
    lazyListState: LazyListState,
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
        lazyListState = lazyListState,
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
    var focused by remember { mutableStateOf(false) }
    Column {
        StripeThemeForConnections {
            PhoneNumberCollectionSection(
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
                isSelected = focused,
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
        defaultStyle = v3Typography.headingXLarge,
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
    var focused by remember { mutableStateOf(false) }
    StripeThemeForConnections {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextFieldSection(
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
                isSelected = focused,
                textFieldController = emailController,
                imeAction = if (showFullForm) ImeAction.Next else ImeAction.Done,
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
            bottomSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden
            ),
            onCloseClick = {},
            onSaveToLink = {},
            onClickableTextClick = {},
            onCloseFromErrorClick = {},
            onSkipClick = {}
        )
    }
}

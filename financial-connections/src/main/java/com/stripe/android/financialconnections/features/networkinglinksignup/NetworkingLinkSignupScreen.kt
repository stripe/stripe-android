package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.LegalDetailsBottomSheetContent
import com.stripe.android.financialconnections.features.common.ListItem
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Payload
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.ViewEffect
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupViewModel.Companion.PANE
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsModalBottomSheetLayout
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.financialconnections.ui.theme.StripeThemeForConnections
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.uicore.elements.DropDown
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection
import kotlinx.coroutines.launch

@Composable
internal fun NetworkingLinkSignupScreen() {
    val viewModel: NetworkingLinkSignupViewModel = paneViewModel(NetworkingLinkSignupViewModel.Companion::factory)
    val parentViewModel = parentViewModel()
    val state = viewModel.stateFlow.collectAsState()
    val topAppBarState by parentViewModel.topAppBarState.collectAsState()
    BackHandler(enabled = true) {}
    val uriHandler = LocalUriHandler.current
    val bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
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
        topAppBarState = topAppBarState,
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
    topAppBarState: TopAppBarState,
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
                topAppBarState = topAppBarState,
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
    topAppBarState: TopAppBarState,
    onSaveToLink: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSkipClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                state = topAppBarState,
                onCloseClick = onCloseClick,
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> FullScreenGenericLoading()
            is Success -> NetworkingLinkSignupLoaded(
                scrollState = scrollState,
                validForm = state.valid,
                payload = payload(),
                lookupAccountSync = state.lookupAccount,
                saveAccountToLinkSync = state.saveAccountToLink,
                showFullForm = state.showFullForm,
                onSaveToLink = onSaveToLink,
                onClickableTextClick = onClickableTextClick,
                onSkipClick = onSkipClick
            )

            is Fail -> UnclassifiedErrorContent { onCloseFromErrorClick(payload.error) }
        }
    }
}

@Composable
private fun NetworkingLinkSignupLoaded(
    scrollState: ScrollState,
    validForm: Boolean,
    payload: Payload,
    saveAccountToLinkSync: Async<FinancialConnectionsSessionManifest>,
    lookupAccountSync: Async<ConsumerSessionLookup>,
    showFullForm: Boolean,
    onClickableTextClick: (String) -> Unit,
    onSaveToLink: () -> Unit,
    onSkipClick: () -> Unit
) {
    val phoneNumberFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showFullForm) {
        if (showFullForm) {
            scrollState.animateScrollToBottom()
            phoneNumberFocusRequester.requestFocus()
        }
    }

    Layout(
        scrollState = scrollState,
        body = {
            Title(payload.content.title)
            Spacer(modifier = Modifier.size(24.dp))

            for (bullet in payload.content.body.bullets) {
                ListItem(
                    bullet = BulletUI.from(bullet),
                    onClickableTextClick = onClickableTextClick
                )
                Spacer(modifier = Modifier.size(16.dp))
            }

            EmailSection(
                showFullForm = showFullForm,
                loading = lookupAccountSync is Loading,
                emailController = payload.emailController,
                enabled = true,
            )

            AnimatedVisibility(showFullForm) {
                PhoneNumberSection(
                    payload = payload,
                    focusRequester = phoneNumberFocusRequester,
                )
            }
        },
        footer = {
            NetworkingLinkSignupFooter(
                payload = payload,
                onClickableTextClick = onClickableTextClick,
                saveAccountToLinkSync = saveAccountToLinkSync,
                validForm = validForm,
                onSaveToLink = onSaveToLink,
                onSkipClick = onSkipClick
            )
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NetworkingLinkSignupFooter(
    payload: Payload,
    onClickableTextClick: (String) -> Unit,
    saveAccountToLinkSync: Async<FinancialConnectionsSessionManifest>,
    validForm: Boolean,
    onSaveToLink: () -> Unit,
    onSkipClick: () -> Unit
) = Column {
    AnnotatedText(
        modifier = Modifier.fillMaxWidth(),
        text = TextResource.Text(fromHtml(payload.content.aboveCta)),
        onClickableTextClick = onClickableTextClick,
        defaultStyle = typography.labelSmall.copy(
            textAlign = TextAlign.Center,
            color = colors.textDefault
        )
    )
    Spacer(modifier = Modifier.size(16.dp))
    FinancialConnectionsButton(
        loading = saveAccountToLinkSync is Loading,
        enabled = validForm,
        type = FinancialConnectionsButton.Type.Primary,
        onClick = onSaveToLink,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(text = payload.content.cta)
    }
    Spacer(modifier = Modifier.size(8.dp))
    FinancialConnectionsButton(
        type = FinancialConnectionsButton.Type.Secondary,
        onClick = onSkipClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .testTag("skip_cta")
    ) {
        Text(text = payload.content.skipCta)
    }
}

@Composable
private fun PhoneNumberSection(
    payload: Payload,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    Column {
        StripeThemeForConnections {
            PhoneNumberCollectionSection(
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
                countryDropdown = {
                    DropDown(
                        controller = payload.phoneController.countryDropdownController,
                        enabled = true,
                        showChevron = false,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.background)
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                },
                isSelected = focused,
                phoneNumberController = payload.phoneController,
                imeAction = ImeAction.Default,
                focusRequester = focusRequester,
                enabled = true,
            )
        }
    }
}

@Composable
private fun Title(title: String) {
    AnnotatedText(
        text = TextResource.Text(fromHtml(title)),
        defaultStyle = typography.headingXLarge,
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
                    color = colors.iconBrand,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

private suspend fun ScrollState.animateScrollToBottom(
    animationSpec: AnimationSpec<Float> = tween(),
) {
    animateScrollBy(Float.MAX_VALUE, animationSpec)
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
            topAppBarState = TopAppBarState(hideStripeLogo = false),
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

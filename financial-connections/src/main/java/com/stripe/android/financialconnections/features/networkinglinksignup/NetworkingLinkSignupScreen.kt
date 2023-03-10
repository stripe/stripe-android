package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.BulletItem
import com.stripe.android.financialconnections.features.common.FormErrorText
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.PaneFooter
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Payload
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupViewModel.Companion.PANE
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.ImageResource
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.StripeThemeForConnections
import com.stripe.android.financialconnections.utils.firstErrorOrNull
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection

@Composable
internal fun NetworkingLinkSignupScreen() {
    val viewModel: NetworkingLinkSignupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
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
            Uninitialized, is Loading -> LoadingContent()
            is Success -> NetworkingLinkSignupLoaded(
                scrollState = scrollState,
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
    Column(
        Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
            Spacer(modifier = Modifier.size(8.dp))
            FirstBullet(payload)
            Spacer(modifier = Modifier.size(12.dp))
            SecondBullet()
            Spacer(modifier = Modifier.size(24.dp))
            EmailSection(
                showFullForm = showFullForm,
                loading = lookupAccountSync is Loading,
                emailController = payload.emailController,
                enabled = true,
            )
            listOf(
                lookupAccountSync,
                saveAccountToLinkSync
            ).firstErrorOrNull()?.let { FormErrorText(error = it) }
            AnimatedVisibility(visible = showFullForm) {
                PhoneNumberSection(
                    payload = payload,
                    onClickableTextClick = onClickableTextClick
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        PaneFooter(elevation = scrollState.elevation) {
            AnimatedVisibility(
                visible = showFullForm
            ) {
                SaveToLinkCta(
                    onClickableTextClick = onClickableTextClick,
                    saveAccountToLinkSync = saveAccountToLinkSync,
                    validForm = validForm,
                    onSaveToLink = onSaveToLink
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            SkipCta(onSkipClick)
        }
    }
}

@Composable
private fun SkipCta(onSkipClick: () -> Unit) {
    FinancialConnectionsButton(
        type = FinancialConnectionsButton.Type.Secondary,
        onClick = onSkipClick,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(text = stringResource(R.string.stripe_networking_signup_cta_negative))
    }
}

@Composable
private fun SaveToLinkCta(
    onClickableTextClick: (String) -> Unit,
    saveAccountToLinkSync: Async<FinancialConnectionsSessionManifest>,
    validForm: Boolean,
    onSaveToLink: () -> Unit
) {
    Column {
        AnnotatedText(
            modifier = Modifier.fillMaxWidth(),
            text = TextResource.StringId(R.string.stripe_networking_signup_terms),
            onClickableTextClick = onClickableTextClick,
            defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                textAlign = TextAlign.Center,
                color = FinancialConnectionsTheme.colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand),
                StringAnnotation.BOLD to FinancialConnectionsTheme.typography.captionEmphasized
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textSecondary),
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
            Text(text = stringResource(R.string.stripe_networking_signup_cta_save))
        }
    }
}

@Composable
private fun PhoneNumberSection(
    payload: Payload,
    onClickableTextClick: (String) -> Unit
) {
    Column {
        StripeThemeForConnections {
            PhoneNumberCollectionSection(
                phoneNumberController = payload.phoneController,
                requestFocusWhenShown = payload.phoneController.initialPhoneNumber.isEmpty(),
                imeAction = ImeAction.Default,
                enabled = true,
            )
        }
        AnnotatedText(
            text = TextResource.StringId(R.string.stripe_networking_signup_phone_number_disclaimer),
            onClickableTextClick = onClickableTextClick,
            defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand),
                StringAnnotation.BOLD to FinancialConnectionsTheme.typography.captionEmphasized
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textSecondary),
            )
        )
    }
}

@Composable
private fun SecondBullet() {
    BulletItem(
        bullet = BulletUI(
            title = null,
            TextResource.StringId(R.string.stripe_networking_signup_bullet2),
            imageResource = ImageResource.Local(R.drawable.stripe_ic_disputeprotection)
        ),
        onClickableTextClick = {}
    )
}

@Composable
private fun FirstBullet(payload: Payload) {
    BulletItem(
        bullet = BulletUI(
            title = null,
            // TODO handle no merchant name case.
            content = TextResource.StringId(
                R.string.stripe_networking_signup_bullet1,
                listOf(payload.merchantName ?: "")
            ),
            imageResource = ImageResource.Local(R.drawable.stripe_ic_convert)
        ),
        onClickableTextClick = {}
    )
}

@Composable
private fun Title() {
    AnnotatedText(
        text = TextResource.Text(stringResource(R.string.stripe_networking_signup_title)),
        defaultStyle = FinancialConnectionsTheme.typography.subtitle,
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.subtitle
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textBrand),
        ),
        onClickableTextClick = {},
    )
}

@Composable
internal fun EmailSection(
    enabled: Boolean,
    emailController: TextFieldController,
    focusRequester: FocusRequester = remember { FocusRequester() },
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
                enabled = enabled,
                modifier = Modifier
                    .focusRequester(focusRequester)
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
                    color = FinancialConnectionsTheme.colors.iconBrand,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
@Preview(group = "NetworkingLinkSignup Pane", name = "Entering email")
internal fun NetworkingLinkSignupScreenEnteringEmailPreview() {
    FinancialConnectionsPreview {
        NetworkingLinkSignupContent(
            state = NetworkingLinkSignupState(
                payload = Success(
                    Payload(
                        merchantName = "Test",
                        emailController = EmailConfig.createController(""),
                        phoneController = PhoneNumberController.createPhoneNumberController(
                            initialValue = "",
                            initiallySelectedCountryCode = null,
                        )
                    )
                ),
                validEmail = null,
                validPhone = null,
                lookupAccount = Uninitialized,
                saveAccountToLink = Uninitialized
            ),
            onCloseClick = {},
            onSaveToLink = {},
            onClickableTextClick = {},
            onCloseFromErrorClick = {},
            onSkipClick = {}
        )
    }
}

@Composable
@Preview(group = "NetworkingLinkSignup Pane", name = "Entering phone")
internal fun NetworkingLinkSignupScreenEnteringPhonePreview() {
    FinancialConnectionsPreview {
        NetworkingLinkSignupContent(
            state = NetworkingLinkSignupState(
                payload = Success(
                    Payload(
                        merchantName = "Test",
                        emailController = EmailConfig.createController("test@test.com"),
                        phoneController = PhoneNumberController.createPhoneNumberController(
                            initialValue = "",
                            initiallySelectedCountryCode = null,
                        )
                    )
                ),
                validEmail = "test@test.com",
                validPhone = null,
                lookupAccount = Success(
                    ConsumerSessionLookup(
                        exists = false,
                        consumerSession = null,
                        errorMessage = null
                    )
                ),
                saveAccountToLink = Uninitialized
            ),
            onCloseClick = {},
            onSaveToLink = {},
            onClickableTextClick = {},
            onCloseFromErrorClick = {},
            onSkipClick = {}
        )
    }
}

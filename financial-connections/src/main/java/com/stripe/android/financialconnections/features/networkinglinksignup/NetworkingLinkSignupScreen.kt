package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Form
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Payload
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.SignUpState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.StripeThemeForConnections
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextFieldSection

@Composable
internal fun NetworkingLinkSignupScreen() {
    val viewModel: NetworkingLinkSignupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    NetworkingLinkSignupContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.RESET) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onSaveToLink = viewModel::onSaveAccount
    )
}

@Composable
private fun NetworkingLinkSignupContent(
    state: NetworkingLinkSignupState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onSaveToLink: () -> Unit
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> LoadingContent()
            is Success -> NetworkingLinkSignupLoaded(
                scrollState = scrollState,
                validForm = state.form.valid(),
                payload = payload(),
                onSaveToLink = onSaveToLink,
                signupState = state.signupState
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
    signupState: SignUpState,
    payload: Payload,
    onSaveToLink: () -> Unit
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
            AnnotatedText(
                text = TextResource.Text("Save your account to Link"),
                defaultStyle = FinancialConnectionsTheme.typography.subtitle,
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.subtitle
                        .toSpanStyle()
                        .copy(color = FinancialConnectionsTheme.colors.textBrand),
                ),
                onClickableTextClick = {},
            )
            StripeThemeForConnections {
                TextFieldSection(
                    textFieldController = payload.emailController,
                    imeAction = ImeAction.Default,
                    enabled = true,
                )
            }
            AnimatedVisibility(
                visible = signupState == SignUpState.InputtingPhoneOrName
            ) {
                StripeThemeForConnections {
                    PhoneNumberCollectionSection(
                        phoneNumberController = payload.phoneController,
                        imeAction = ImeAction.Default,
                        enabled = true,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = 24.dp
            )
        ) {
            AnimatedVisibility(
                visible = signupState == SignUpState.InputtingPhoneOrName
            ) {
                FinancialConnectionsButton(
                    enabled = validForm,
                    type = FinancialConnectionsButton.Type.Primary,
                    onClick = onSaveToLink,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = "Save to Link")
                }
            }
            FinancialConnectionsButton(
                type = FinancialConnectionsButton.Type.Secondary,
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "No thanks")
            }
        }
    }
}

@Composable
@Preview
internal fun NetworkingLinkSignupScreenPreview() {
    FinancialConnectionsPreview {
        NetworkingLinkSignupContent(
            state = NetworkingLinkSignupState(
                signupState = SignUpState.InputtingPhoneOrName,
                payload = Success(
                    Payload(
                        emailController = EmailConfig.createController(""),
                        phoneController = PhoneNumberController.createPhoneNumberController(
                            initialValue = "",
                            initiallySelectedCountryCode = null,
                        )
                    )
                ),
                form = Form(),
                saveAccountToLink = Uninitialized
            ),
            onCloseClick = {},
            onSaveToLink = {},
            onCloseFromErrorClick = {}
        )
    }
}

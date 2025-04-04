package com.stripe.android.link.ui.signup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.LinkTermsType
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ProgressIndicatorTestTag
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay

@Composable
internal fun SignUpScreen(
    viewModel: SignUpViewModel,
) {
    val signUpScreenState by viewModel.state.collectAsState()

    SignUpBody(
        emailController = viewModel.emailController,
        phoneNumberController = viewModel.phoneNumberController,
        nameController = viewModel.nameController,
        signUpScreenState = signUpScreenState,
        onSignUpClick = viewModel::onSignUpClick
    )
}

@Composable
internal fun SignUpBody(
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpScreenState: SignUpScreenState,
    onSignUpClick: () -> Unit
) {
    var didFocusField by rememberSaveable { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }

    if (!didFocusField && signUpScreenState.showKeyboardOnOpen) {
        LaunchedEffect(Unit) {
            delay(LINK_DEFAULT_ANIMATION_DELAY_MILLIS)
            emailFocusRequester.requestFocus()
            didFocusField = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.stripe_link_sign_up_header),
            modifier = Modifier
                .testTag(SIGN_UP_HEADER_TAG)
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = stringResource(R.string.stripe_link_sign_up_message),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 30.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSecondary
        )
        StripeThemeForLink {
            EmailCollectionSection(
                enabled = true,
                emailController = emailController,
                signUpScreenState = signUpScreenState,
                focusRequester = emailFocusRequester,
            )
        }
        AnimatedVisibility(
            visible = signUpScreenState.signUpState != SignUpState.InputtingRemainingFields &&
                signUpScreenState.errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ErrorText(
                text = signUpScreenState.errorMessage?.resolve(LocalContext.current).orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SIGN_UP_ERROR_TAG)
            )
        }
        AnimatedVisibility(visible = signUpScreenState.signUpState == SignUpState.InputtingRemainingFields) {
            SecondaryFields(
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpScreenState = signUpScreenState,
                onSignUpClick = onSignUpClick
            )
        }
    }
}

@Composable
private fun EmailCollectionSection(
    enabled: Boolean,
    emailController: TextFieldController,
    signUpScreenState: SignUpScreenState,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        TextFieldSection(
            textFieldController = emailController,
            modifier = Modifier
                .padding(vertical = 8.dp),
        ) {
            TextField(
                modifier = Modifier
                    .focusRequester(focusRequester),
                textFieldController = emailController,
                imeAction = if (signUpScreenState.signUpState == SignUpState.InputtingRemainingFields) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                },
                enabled = enabled && signUpScreenState.signUpState != SignUpState.VerifyingEmail,
            )
        }
        if (signUpScreenState.signUpState == SignUpState.VerifyingEmail) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .padding(
                        start = 0.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    )
                    .semantics {
                        testTag = ProgressIndicatorTestTag
                    },
                color = MaterialTheme.linkColors.progressIndicator,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun SecondaryFields(
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpScreenState: SignUpScreenState,
    onSignUpClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier = Modifier.fillMaxWidth()) {
        StripeThemeForLink {
            PhoneNumberCollectionSection(
                enabled = true,
                phoneNumberController = phoneNumberController,
                requestFocusWhenShown = phoneNumberController.initialPhoneNumber.isEmpty(),
                imeAction = if (signUpScreenState.requiresNameCollection) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                }
            )

            if (signUpScreenState.requiresNameCollection) {
                TextFieldSection(
                    modifier = Modifier.padding(vertical = 8.dp),
                    textFieldController = nameController,
                ) {
                    TextField(
                        textFieldController = nameController,
                        imeAction = ImeAction.Done,
                        enabled = true,
                    )
                }
            }

            LinkTerms(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                type = LinkTermsType.Full,
            )
        }
        AnimatedVisibility(visible = signUpScreenState.errorMessage != null) {
            ErrorText(
                text = signUpScreenState.errorMessage?.resolve(LocalContext.current).orEmpty(),
                modifier = Modifier
                    .testTag(SIGN_UP_ERROR_TAG)
                    .fillMaxWidth()
            )
        }
        PrimaryButton(
            label = stringResource(R.string.stripe_link_sign_up),
            state = if (signUpScreenState.signUpEnabled) {
                PrimaryButtonState.Enabled
            } else {
                PrimaryButtonState.Disabled
            },
            onButtonClick = {
                onSignUpClick()
                keyboardController?.hide()
            }
        )
    }
}

internal const val SIGN_UP_HEADER_TAG = "signUpHeaderTag"
internal const val SIGN_UP_ERROR_TAG = "signUpErrorTag"

@Preview
@Composable
private fun SignUpScreenPreview() {
    DefaultLinkTheme {
        Surface {
            SignUpBody(
                emailController = EmailConfig.createController("email"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("My Name"),
                signUpScreenState = SignUpScreenState(
                    merchantName = "Example, Inc.",
                    signUpEnabled = false,
                    signUpState = SignUpState.InputtingRemainingFields,
                    requiresNameCollection = true,
                    showKeyboardOnOpen = false,
                ),
                onSignUpClick = {}
            )
        }
    }
}

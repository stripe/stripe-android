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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.progressIndicatorTestTag
import com.stripe.android.ui.core.elements.EmailConfig
import com.stripe.android.ui.core.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection

@Preview
@Composable
private fun SignUpBodyPreview() {
    DefaultLinkTheme {
        Surface {
            SignUpBody(
                merchantName = "Example, Inc.",
                emailController = EmailConfig.createController("email"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("My Name"),
                signUpState = SignUpState.InputtingPhoneOrName,
                isReadyToSignUp = false,
                requiresNameCollection = true,
                errorMessage = null,
                onSignUpClick = {}
            )
        }
    }
}

@Composable
internal fun SignUpBody(
    injector: NonFallbackInjector,
) {
    val signUpViewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModel.Factory(injector)
    )

    val signUpState by signUpViewModel.signUpState.collectAsState()
    val isReadyToSignUp by signUpViewModel.isReadyToSignUp.collectAsState()
    val errorMessage by signUpViewModel.errorMessage.collectAsState()

    SignUpBody(
        merchantName = signUpViewModel.merchantName,
        emailController = signUpViewModel.emailController,
        phoneNumberController = signUpViewModel.phoneController,
        nameController = signUpViewModel.nameController,
        signUpState = signUpState,
        isReadyToSignUp = isReadyToSignUp,
        requiresNameCollection = signUpViewModel.requiresNameCollection,
        errorMessage = errorMessage,
        onSignUpClick = signUpViewModel::onSignUpClick
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SignUpBody(
    merchantName: String,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    nameController: TextFieldController,
    signUpState: SignUpState,
    isReadyToSignUp: Boolean,
    requiresNameCollection: Boolean,
    errorMessage: ErrorMessage?,
    onSignUpClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    ScrollableTopLevelColumn {
        Text(
            text = stringResource(R.string.sign_up_header),
            modifier = Modifier
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            text = stringResource(R.string.sign_up_message, merchantName),
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
                signUpState = signUpState
            )
        }
        AnimatedVisibility(
            visible = signUpState != SignUpState.InputtingPhoneOrName &&
                errorMessage != null
        ) {
            ErrorText(
                text = errorMessage?.getMessage(LocalContext.current.resources).orEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(visible = signUpState == SignUpState.InputtingPhoneOrName) {
            Column(modifier = Modifier.fillMaxWidth()) {
                StripeThemeForLink {
                    PhoneNumberCollectionSection(
                        enabled = true,
                        phoneNumberController = phoneNumberController,
                        requestFocusWhenShown = phoneNumberController.initialPhoneNumber.isEmpty(),
                        imeAction = if (requiresNameCollection) {
                            ImeAction.Next
                        } else {
                            ImeAction.Done
                        }
                    )

                    if (requiresNameCollection) {
                        TextFieldSection(
                            textFieldController = nameController,
                            imeAction = ImeAction.Done,
                            enabled = true
                        )
                    }

                    LinkTerms(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                AnimatedVisibility(visible = errorMessage != null) {
                    ErrorText(
                        text = errorMessage?.getMessage(LocalContext.current.resources).orEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PrimaryButton(
                    label = stringResource(R.string.sign_up),
                    state = if (isReadyToSignUp) {
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
    }
}

@Composable
internal fun EmailCollectionSection(
    enabled: Boolean,
    emailController: TextFieldController,
    signUpState: SignUpState,
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
            imeAction = if (signUpState == SignUpState.InputtingPhoneOrName) {
                ImeAction.Next
            } else {
                ImeAction.Done
            },
            enabled = enabled && signUpState != SignUpState.VerifyingEmail,
            modifier = Modifier
                .focusRequester(focusRequester)
        )
        if (signUpState == SignUpState.VerifyingEmail) {
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
                        testTag = progressIndicatorTestTag
                    },
                color = MaterialTheme.linkColors.progressIndicator,
                strokeWidth = 2.dp
            )
        }
    }
}

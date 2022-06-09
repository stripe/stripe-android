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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.stripe.android.link.R
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.PaymentsThemeForLink
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.progressIndicatorTestTag
import com.stripe.android.ui.core.elements.PhoneNumberCollectionSection
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.elements.TextFieldSection

@Preview
@Composable
private fun SignUpBodyPreview() {
    DefaultLinkTheme {
        Surface {
            SignUpBody(
                merchantName = "Example, Inc.",
                emailController = SimpleTextFieldController.createEmailSectionController("email"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                signUpState = SignUpState.InputtingPhone,
                isReadyToSignUp = false,
                errorMessage = null,
                onSignUpClick = {}
            )
        }
    }
}

@Composable
internal fun SignUpBody(
    injector: NonFallbackInjector,
    email: String?
) {
    val signUpViewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModel.Factory(
            injector,
            email
        )
    )

    val signUpState by signUpViewModel.signUpState.collectAsState()
    val isReadyToSignUp by signUpViewModel.isReadyToSignUp.collectAsState(false)
    val errorMessage by signUpViewModel.errorMessage.collectAsState()

    SignUpBody(
        merchantName = signUpViewModel.merchantName,
        emailController = signUpViewModel.emailController,
        phoneNumberController = signUpViewModel.phoneController,
        signUpState = signUpState,
        isReadyToSignUp = isReadyToSignUp,
        errorMessage = errorMessage,
        onSignUpClick = signUpViewModel::onSignUpClick
    )
}

@Composable
internal fun SignUpBody(
    merchantName: String,
    emailController: TextFieldController,
    phoneNumberController: PhoneNumberController,
    signUpState: SignUpState,
    isReadyToSignUp: Boolean,
    errorMessage: ErrorMessage?,
    onSignUpClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    ScrollableTopLevelColumn {
        Text(
            text = stringResource(
                if (signUpState == SignUpState.InputtingPhone) {
                    R.string.sign_up_header_new_user
                } else {
                    R.string.sign_up_header
                }
            ),
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
        PaymentsThemeForLink {
            EmailCollectionSection(
                enabled = true,
                emailController = emailController,
                signUpState = signUpState
            )
        }
        AnimatedVisibility(
            visible = signUpState == SignUpState.InputtingPhone
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PaymentsThemeForLink {
                    PhoneNumberCollectionSection(
                        enabled = true,
                        phoneNumberController = phoneNumberController,
                        requestFocusWhenShown = phoneNumberController.initialPhoneNumber.isEmpty()
                    )
                    LinkTerms(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                errorMessage?.let {
                    ErrorText(text = it.getMessage(LocalContext.current.resources))
                }
                PrimaryButton(
                    label = stringResource(R.string.sign_up),
                    state = if (isReadyToSignUp) {
                        PrimaryButtonState.Enabled
                    } else {
                        PrimaryButtonState.Disabled
                    }
                ) {
                    onSignUpClick()
                    keyboardController?.hide()
                }
            }
        }
    }
}

@Composable
internal fun EmailCollectionSection(
    enabled: Boolean,
    emailController: TextFieldController,
    signUpState: SignUpState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        TextFieldSection(
            textFieldController = emailController,
            imeAction = if (signUpState == SignUpState.InputtingPhone) {
                ImeAction.Next
            } else {
                ImeAction.Done
            },
            enabled = enabled && signUpState != SignUpState.VerifyingEmail
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
                color = MaterialTheme.linkColors.buttonLabel,
                strokeWidth = 2.dp
            )
        }
    }
}

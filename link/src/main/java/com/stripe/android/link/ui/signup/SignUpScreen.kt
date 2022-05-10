package com.stripe.android.link.ui.signup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.R
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkTextFieldColors
import com.stripe.android.link.ui.LinkTerms
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.progressIndicatorTestTag
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.SectionCard
import com.stripe.android.ui.core.elements.SectionController
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionElementUI
import com.stripe.android.ui.core.elements.SectionFieldElement

@Preview
@Composable
private fun SignUpBodyPreview() {
    DefaultLinkTheme {
        Surface {
            SignUpBody(
                merchantName = "Example, Inc.",
                emailElement = EmailElement(
                    initialValue = "email"
                ),
                signUpState = SignUpState.InputtingPhone,
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

    val signUpStatus by signUpViewModel.signUpState.collectAsState(SignUpState.InputtingEmail)

    SignUpBody(
        merchantName = signUpViewModel.merchantName,
        emailElement = signUpViewModel.emailElement,
        signUpState = signUpStatus,
        onSignUpClick = signUpViewModel::onSignUpClick
    )
}

@Composable
internal fun SignUpBody(
    merchantName: String,
    emailElement: EmailElement,
    signUpState: SignUpState,
    onSignUpClick: (String) -> Unit
) {
    if (signUpState == SignUpState.VerifyingEmail) {
        LocalFocusManager.current.clearFocus()
    }

    var phoneNumber by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        EmailCollectionSection(
            emailElement = emailElement,
            signUpState = signUpState
        )
        AnimatedVisibility(
            visible = signUpState == SignUpState.InputtingPhone
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // TODO(brnunes-stripe): Migrate to phone number collection element
                PhoneCollectionSection(
                    phoneNumber = phoneNumber,
                    onPhoneNumberChanged = {
                        phoneNumber = it
                    }
                )
                LinkTerms(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
                PrimaryButton(
                    label = stringResource(R.string.sign_up),
                    state = if (phoneNumber.length == 10) {
                        PrimaryButtonState.Enabled
                    } else {
                        PrimaryButtonState.Disabled
                    }
                ) {
                    onSignUpClick(phoneNumber)
                    keyboardController?.hide()
                }
            }
        }
    }
}

@Composable
internal fun EmailCollectionSection(
    emailElement: SectionFieldElement,
    signUpState: SignUpState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        SectionElementUI(
            enabled = signUpState != SignUpState.VerifyingEmail,
            element = SectionElement(
                identifier = IdentifierSpec.Email,
                fields = listOf(emailElement),
                controller = SectionController(
                    null,
                    listOf(emailElement.sectionFieldErrorController())
                )
            ),
            emptyList(),
            emailElement.identifier
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

@Composable
internal fun PhoneCollectionSection(
    phoneNumber: String,
    onPhoneNumberChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionCard {
            TextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = phoneNumber,
                onValueChange = onPhoneNumberChanged,
                label = {
                    Text(text = "Mobile Number")
                },
                shape = MaterialTheme.shapes.medium,
                colors = linkTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Go
                ),
                singleLine = true
            )
        }
    }
}

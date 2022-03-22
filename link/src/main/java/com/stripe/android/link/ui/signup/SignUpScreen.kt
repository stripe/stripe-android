package com.stripe.android.link.ui.signup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.stripe.android.link.theme.linkTextFieldColors
import com.stripe.android.ui.core.elements.EmailSpec
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
        SignUpBody(
            merchantName = "Example, Inc.",
            emailElement = EmailSpec.transform("email"),
            signUpState = SignUpState.InputtingPhone,
            onSignUpClick = {}
        )
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
    emailElement: SectionFieldElement,
    signUpState: SignUpState,
    onSignUpClick: (String) -> Unit
) {
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
            PhoneCollectionSection(onSignUpClick)
        }
    }
}

@Composable
private fun EmailCollectionSection(
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
                identifier = IdentifierSpec.Generic("email"),
                fields = listOf(emailElement),
                controller = SectionController(
                    null,
                    listOf(emailElement.sectionFieldErrorController())
                )
            ),
            emptyList()
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
                        testTag = "CircularProgressIndicator"
                    },
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun PhoneCollectionSection(
    onSignUpClick: (String) -> Unit
) {
    // TODO(brnunes-stripe): Migrate to phone number collection element
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionCard {
            TextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = phone,
                onValueChange = {
                    phone = it
                },
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
        Text(
            text = stringResource(R.string.sign_up_terms),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption
        )
        TextButton(
            onClick = {
                onSignUpClick(phone)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = phone.length == 10,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
        ) {
            Text(
                text = stringResource(R.string.sign_up),
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.onPrimary
            )
        }
    }
}

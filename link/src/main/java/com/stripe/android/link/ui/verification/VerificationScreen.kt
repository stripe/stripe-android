package com.stripe.android.link.ui.verification

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.R
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkTextFieldColors
import com.stripe.android.ui.core.elements.SectionCard

@Preview
@Composable
private fun VerificationBodyPreview() {
    DefaultLinkTheme {
        VerificationBody(
            redactedPhoneNumber = "+1********23",
            email = "test@stripe.com",
            onCodeEntered = { },
            onBack = { },
            onChangeEmailClick = { },
            onResendCodeClick = { }
        )
    }
}

@Composable
internal fun VerificationBody(
    linkAccount: LinkAccount,
    injector: NonFallbackInjector
) {
    val viewModel: VerificationViewModel = viewModel(
        factory = VerificationViewModel.Factory(
            linkAccount,
            injector
        )
    )

    VerificationBody(
        redactedPhoneNumber = viewModel.linkAccount.redactedPhoneNumber,
        email = viewModel.linkAccount.email,
        onCodeEntered = viewModel::onVerificationCodeEntered,
        onBack = viewModel::onBack,
        onChangeEmailClick = viewModel::onChangeEmailClicked,
        onResendCodeClick = viewModel::onResendCodeClicked
    )
}

@Composable
internal fun VerificationBody(
    redactedPhoneNumber: String,
    email: String,
    onCodeEntered: (String) -> Unit,
    onBack: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onResendCodeClick: () -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.verification_header),
            modifier = Modifier
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            text = stringResource(R.string.verification_message, redactedPhoneNumber),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 30.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSecondary
        )
        VerificationCodeInput(onCodeEntered)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.verification_not_email, email),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSecondary
            )
            Text(
                text = stringResource(id = R.string.verification_change_email),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable(onClick = onChangeEmailClick),
                style = MaterialTheme.typography.body2
                    .merge(TextStyle(textDecoration = TextDecoration.Underline)),
                color = MaterialTheme.colors.onSecondary
            )
        }
        TextButton(
            onClick = onResendCodeClick,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.linkColors.disabledText,
                    shape = MaterialTheme.shapes.medium
                ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background
            )
        ) {
            Text(
                text = stringResource(id = R.string.verification_resend),
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.onPrimary
            )
        }
    }
}

@Composable
private fun VerificationCodeInput(
    onCodeEntered: (String) -> Unit
) {
    // TODO(brnunes-stripe): Migrate to OTP collection element
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionCard {
            TextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = code,
                onValueChange = {
                    code = it
                    if (code.length == 6) {
                        onCodeEntered(code)
                    }
                },
                label = {
                    Text(text = "<Code>")
                },
                shape = MaterialTheme.shapes.medium,
                colors = linkTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go
                ),
                singleLine = true
            )
        }
    }
}

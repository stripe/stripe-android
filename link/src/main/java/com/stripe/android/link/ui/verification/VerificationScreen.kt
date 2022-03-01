package com.stripe.android.link.ui.verification

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkTextFieldColors
import com.stripe.android.ui.core.elements.OTPElementUI

@Preview
@Composable
private fun VerificationBodyPreview() {
    DefaultLinkTheme {
        VerificationBody(
            redactedPhoneNumber = "+1********23",
            onCodeEntered = { },
            onResendCodeClick = { }
        )
    }
}

@Composable
internal fun VerificationBody(
    application: Application,
    starterArgsSupplier: () -> LinkActivityContract.Args
) {
    val viewModel: VerificationViewModel = viewModel(
        factory = VerificationViewModel.Factory(
            application,
            starterArgsSupplier
        )
    )

    VerificationBody(
        redactedPhoneNumber = viewModel.linkAccount.redactedPhoneNumber,
        onCodeEntered = viewModel::onVerificationCodeEntered,
        onResendCodeClick = viewModel::onResendCodeClicked
    )
}

@Composable
internal fun VerificationBody(
    redactedPhoneNumber: String,
    onCodeEntered: (String) -> Unit,
    onResendCodeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.verification_header),
            modifier = Modifier
                .padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2
        )
        Text(
            text = stringResource(R.string.verification_message, redactedPhoneNumber),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 30.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1
        )
        OTPElementUI(
            colors = linkTextFieldColors(),
            onComplete = { onCodeEntered(it) }
        )
        TextButton(
            onClick = onResendCodeClick,
            modifier = Modifier.padding(top = 30.dp)
        ) {
            Text(
                text = stringResource(id = R.string.verification_resend),
                style = MaterialTheme.typography.button
            )
        }
    }
}

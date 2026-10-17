package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.crypto.onramp.example.AUTHENTICATE_BUTTON_TAG

@Composable
internal fun AuthenticationScreen(
    email: String,
    onAuthenticate: (String) -> Unit,
    onUpdatePhoneNumber: (String) -> Unit,
    onBack: () -> Unit
) {
    Column {
        Text(
            text = "Authenticate Link User",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { },
            label = { Text("Email Address") },
            enabled = false,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        AuthenticateSection(onAuthenticate = onAuthenticate)

        Spacer(modifier = Modifier.height(24.dp))

        UpdatePhoneNumberSection(onUpdatePhoneNumber = onUpdatePhoneNumber)

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Sign in")
        }
    }
}

@Composable
internal fun AuthenticateSection(
    onAuthenticate: (String) -> Unit
) {
    var oauthScopes by remember { mutableStateOf(DEFAULT_OAUTH_SCOPES) }

    OutlinedTextField(
        value = oauthScopes,
        onValueChange = { oauthScopes = it },
        label = { Text("Request OAuth scopes (optional)") },
        placeholder = { Text("userinfo:read,kyc:share") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )

    Button(
        onClick = { onAuthenticate(oauthScopes) },
        modifier = Modifier
            .testTag(AUTHENTICATE_BUTTON_TAG)
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text("Authenticate")
    }
}

@Composable
private fun UpdatePhoneNumberSection(
    onUpdatePhoneNumber: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Update Phone Number",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number (E.164 format)") },
            placeholder = { Text("+1234567890") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { onUpdatePhoneNumber(phoneNumber) },
            enabled = phoneNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Phone Number")
        }
    }
}

private const val DEFAULT_OAUTH_SCOPES =
    "kyc.status:read,crypto:ramp,auth.persist_login:read"

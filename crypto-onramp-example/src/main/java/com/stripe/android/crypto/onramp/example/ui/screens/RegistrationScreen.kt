package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.stripe.android.crypto.onramp.example.REGISTRATION_COUNTRY_TAG
import com.stripe.android.crypto.onramp.example.REGISTRATION_FULL_NAME_TAG
import com.stripe.android.crypto.onramp.example.REGISTRATION_PHONE_TAG
import com.stripe.android.crypto.onramp.example.REGISTRATION_REGISTER_BUTTON_TAG

@Composable
internal fun RegistrationScreen(
    initialEmail: String,
    onRegister: (String, String, String, String?) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Register New Link User",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        RegistrationFields(
            email = email,
            onEmailChange = { email = it },
            phone = phone,
            onPhoneChange = { phone = it },
            country = country,
            onCountryChange = { country = it },
            fullName = fullName,
            onFullNameChange = { fullName = it }
        )

        RegistrationButtons(
            onBack = onBack,
            onRegister = { onRegister(email, phone, country, fullName) }
        )
    }
}

@Composable
private fun RegistrationFields(
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit,
    fullName: String,
    onFullNameChange: (String) -> Unit
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email Address *") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Phone Number (E.164 format) *") },
        placeholder = { Text("+1234567890") },
        singleLine = true,
        modifier = Modifier
            .testTag(REGISTRATION_PHONE_TAG)
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    OutlinedTextField(
        value = country,
        onValueChange = onCountryChange,
        label = { Text("Country Code *") },
        placeholder = { Text("US") },
        singleLine = true,
        modifier = Modifier
            .testTag(REGISTRATION_COUNTRY_TAG)
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    OutlinedTextField(
        value = fullName,
        onValueChange = onFullNameChange,
        label = { Text("Full Name (optional)") },
        singleLine = true,
        modifier = Modifier
            .testTag(REGISTRATION_FULL_NAME_TAG)
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    )
}

@Composable
private fun RegistrationButtons(
    onBack: () -> Unit,
    onRegister: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.weight(1f)
        ) {
            Text("Back")
        }

        Button(
            onClick = onRegister,
            modifier = Modifier
                .testTag(REGISTRATION_REGISTER_BUTTON_TAG)
                .weight(1f)
        ) {
            Text("Register")
        }
    }
}

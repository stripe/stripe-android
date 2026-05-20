package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stripe.android.crypto.onramp.example.LOGIN_EMAIL_TAG
import com.stripe.android.crypto.onramp.example.LOGIN_LOGIN_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.LOGIN_PASSWORD_TAG
import com.stripe.android.crypto.onramp.example.LOGIN_REGISTER_BUTTON_TAG

@Composable
internal fun LoginSignupScreen(
    onRegister: (String, String) -> Unit,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .testTag(LOGIN_EMAIL_TAG)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .onPreviewKeyEvent {
                    if (it.key != Key.Tab) {
                        return@onPreviewKeyEvent false
                    }

                    focusManager.moveFocus(FocusDirection.Next)
                    true
                }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .testTag(LOGIN_PASSWORD_TAG)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LOGIN_LOGIN_BUTTON_TAG)
                .padding(bottom = 16.dp)
        ) {
            Text("Login")
        }

        Button(
            onClick = { onRegister(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LOGIN_REGISTER_BUTTON_TAG)
        ) {
            Text("Register")
        }
    }
}

package com.stripe.android.identity.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.example.NetworkedIdentityExampleState
import com.stripe.android.identity.example.R
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController

private typealias StateUpdate = ((NetworkedIdentityExampleState) -> NetworkedIdentityExampleState) -> Unit

/**
 * Networked Identity playground controls: the debug values the VerificationPage doesn't return yet,
 * plus a Link sign-in that produces a session handoff from outside Identity.
 */
@Composable
internal fun NetworkedIdentitySettingsUI(
    state: NetworkedIdentityExampleState,
    onStateChanged: StateUpdate,
    onSendCode: () -> Unit,
    onVerify: () -> Unit,
    onClear: () -> Unit,
    onCreateAccount: (phoneNumber: String, country: String) -> Unit,
) {
    Divider()
    Column(modifier = Modifier.padding(horizontal = 10.dp)) {
        Text(
            text = stringResource(R.string.ni_section),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        DebugFields(state, onStateChanged)
        LinkSignIn(state, onStateChanged, onSendCode, onVerify, onClear)
        if (state.needsSignUp) {
            SignUpFields(busy = state.busy, onCreateAccount = onCreateAccount)
        }
        state.status?.let { status ->
            Text(text = status, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun DebugFields(state: NetworkedIdentityExampleState, onStateChanged: StateUpdate) {
    OutlinedTextField(
        value = state.merchantPublishableKey,
        onValueChange = { value -> onStateChanged { it.copy(merchantPublishableKey = value) } },
        label = { Text(stringResource(R.string.ni_merchant_pk)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = state.seedSavedDocuments,
            onCheckedChange = { value -> onStateChanged { it.copy(seedSavedDocuments = value) } }
        )
        Text(text = stringResource(R.string.ni_seed_docs))
    }
}

@Composable
private fun LinkSignIn(
    state: NetworkedIdentityExampleState,
    onStateChanged: StateUpdate,
    onSendCode: () -> Unit,
    onVerify: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = state.linkEmail,
        onValueChange = { value -> onStateChanged { it.copy(linkEmail = value) } },
        label = { Text(stringResource(R.string.ni_link_email)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (state.awaitingCode) {
        OutlinedTextField(
            value = state.code,
            onValueChange = { value -> onStateChanged { it.copy(code = value) } },
            label = { Text(stringResource(R.string.ni_code)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onSendCode, enabled = !state.busy) {
            Text(text = stringResource(R.string.ni_send_code))
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (state.awaitingCode) {
            Button(onClick = onVerify, enabled = !state.busy) {
                Text(text = stringResource(R.string.ni_verify))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (state.handoff != null) {
            Button(onClick = onClear, enabled = !state.busy) {
                Text(text = stringResource(R.string.ni_clear_session))
            }
        }
    }
}

@Composable
private fun SignUpFields(busy: Boolean, onCreateAccount: (phoneNumber: String, country: String) -> Unit) {
    val phoneController = remember { PhoneNumberController.createPhoneNumberController() }
    val complete by phoneController.isComplete.collectAsState()
    PhoneNumberCollectionSection(
        enabled = !busy,
        phoneNumberController = phoneController,
    )
    Button(
        onClick = { onCreateAccount(phoneController.rawFieldValue.value, phoneController.getCountryCode()) },
        enabled = complete && !busy,
    ) {
        Text(text = stringResource(R.string.ni_create_account))
    }
}

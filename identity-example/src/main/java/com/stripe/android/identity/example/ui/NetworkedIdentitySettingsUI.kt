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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.example.NetworkedIdentityExampleState
import com.stripe.android.identity.example.R

private typealias StateUpdate = ((NetworkedIdentityExampleState) -> NetworkedIdentityExampleState) -> Unit

/**
 * Networked Identity playground controls: the debug values the VerificationPage doesn't return yet,
 * plus a Link sign-in that produces a session handoff from outside Identity.
 */
@Composable
internal fun NetworkedIdentitySettingsUI(
    state: NetworkedIdentityExampleState,
    onStateChanged: StateUpdate,
    onSignIn: () -> Unit,
    onClear: () -> Unit,
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
        LinkSignIn(state, onStateChanged, onSignIn, onClear)
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
    onSignIn: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = state.linkEmail,
        onValueChange = { value -> onStateChanged { it.copy(linkEmail = value) } },
        label = { Text(stringResource(R.string.ni_link_email)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onSignIn, enabled = !state.busy) {
            Text(text = stringResource(R.string.ni_sign_in))
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (state.handoff != null) {
            Button(onClick = onClear, enabled = !state.busy) {
                Text(text = stringResource(R.string.ni_clear_session))
            }
        }
    }
}

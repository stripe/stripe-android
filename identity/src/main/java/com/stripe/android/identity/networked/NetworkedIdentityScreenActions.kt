package com.stripe.android.identity.networked

internal data class NetworkedIdentityScreenActions(
    val onSelectDocument: (String) -> Unit,
    val onShareDocument: () -> Unit,
    val onContinue: () -> Unit,
    val onManualCapture: () -> Unit,
    val onCancel: () -> Unit,
)

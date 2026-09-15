package com.stripe.android.identity.networked

internal data class NetworkedIdentityScreenActions(
    val onSubmitEmail: (String) -> Unit,
    val onSubmitPhone: (phoneNumber: String, country: String) -> Unit,
    val onSubmitOtp: (String) -> Unit,
    val onResendOtp: () -> Unit,
    val onSelectDocument: (String) -> Unit,
    val onShareDocument: () -> Unit,
    val onContinue: () -> Unit,
    val onManualCapture: () -> Unit,
    val onCancel: () -> Unit,
)

package com.stripe.android.link.ui.verification

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString

@Immutable
internal data class VerificationViewState(
    val isProcessing: Boolean,
    val requestFocus: Boolean,
    val errorMessage: ResolvableString?,
    val isSendingNewCode: Boolean,
    val didSendNewCode: Boolean,
    val redactedPhoneNumber: String,
    val email: String,
    val isDialog: Boolean
)

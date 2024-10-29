package com.stripe.android.link.ui.verification

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString

@Immutable
internal data class VerificationViewState(
    val isProcessing: Boolean = false,
    val requestFocus: Boolean = true,
    val errorMessage: ResolvableString? = null,
    val isSendingNewCode: Boolean = false,
    val didSendNewCode: Boolean = false,
    val redactedPhoneNumber: String = "",
    val email: String = ""
)
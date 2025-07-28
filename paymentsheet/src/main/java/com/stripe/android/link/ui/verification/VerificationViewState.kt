package com.stripe.android.link.ui.verification

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.wallet.DefaultPaymentUI
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
internal data class VerificationViewState(
    val isProcessing: Boolean,
    val requestFocus: Boolean,
    val errorMessage: ResolvableString?,
    val isSendingNewCode: Boolean,
    val didSendNewCode: Boolean,
    val redactedPhoneNumber: String,
    val email: String,
    val isDialog: Boolean,
    val defaultPayment: DefaultPaymentUI?
) : Parcelable

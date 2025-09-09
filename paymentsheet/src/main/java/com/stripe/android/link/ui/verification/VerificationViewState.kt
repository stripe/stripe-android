package com.stripe.android.link.ui.verification

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.wallet.DefaultPaymentUI
import com.stripe.android.model.ConsentUi
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
internal data class VerificationViewState(
    val isWeb: Boolean,
    val isProcessing: Boolean,
    val requestFocus: Boolean,
    val errorMessage: ResolvableString?,
    val isSendingNewCode: Boolean,
    val didSendNewCode: Boolean,
    val redactedPhoneNumber: String,
    val email: String,
    val isDialog: Boolean,
    val allowLogout: Boolean,
    val defaultPayment: DefaultPaymentUI?,
    val consentSection: ConsentUi.ConsentSection? = null,
) : Parcelable

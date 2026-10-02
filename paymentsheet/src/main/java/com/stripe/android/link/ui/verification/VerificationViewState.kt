package com.stripe.android.link.ui.verification

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.wallet.DefaultPaymentUI
import com.stripe.android.model.ConsentUi
import com.stripe.android.model.LinkBrand
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
    val allowLogout: Boolean,
    val defaultPayment: DefaultPaymentUI?,
    val consentSection: ConsentUi.ConsentSection? = null,
    val isProcessingWebAuth: Boolean = false,
    val linkBrand: LinkBrand,
    /**
     * The state of the multi-factor auth flow, or null when only SMS OTP is supported.
     */
    val authFlow: AuthFlowViewState?,
) : Parcelable {

    @Immutable
    @Parcelize
    data class AuthFlowViewState(
        val screen: LinkAuthFlowState.Screen,
        val recipient: String,
        val canGoBack: Boolean,
        val codeEntryEnabled: Boolean,
        val actions: List<LinkAuthFlowState.Action>,
        val canResend: Boolean,
        val isResending: Boolean,
        val resendSecondsRemaining: Int,
        val phoneNumberLastTwoDigits: String?,
    ) : Parcelable
}

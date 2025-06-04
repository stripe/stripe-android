package com.stripe.android.link.verification

import android.os.Parcelable
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.ui.verification.VerificationViewState
import kotlinx.parcelize.Parcelize

/**
 * Unified state for Link embedded functionality including verification and payment method selection
 */
@Parcelize
internal data class LinkEmbeddedState(
    /**
     * Current verification state representing the different stages in the verification process.
     */
    val verificationState: VerificationState = VerificationState.Loading,

    /**
     * Preserved Link payment method selection for re-selection after switching payment methods
     */
    val preservedPaymentMethod: LinkPaymentMethod? = null,
) : Parcelable

/**
 * Represents the different states of the verification process
 */
internal sealed class VerificationState : Parcelable {
    /**
     * Initial state while waiting for account information
     */
    @Parcelize
    object Loading : VerificationState()

    /**
     * Verification is required and the UI should show the verification form
     */
    @Parcelize
    data class Verifying(val viewState: VerificationViewState) : VerificationState()

    /**
     * Verification is completed or not needed, showing the normal Link button
     */
    @Parcelize
    object Resolved : VerificationState()
}

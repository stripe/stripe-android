package com.stripe.android.link.verification

import android.os.Parcelable
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.verification.VerificationViewState
import kotlinx.parcelize.Parcelize

/**
 * Unified state for Link embedded functionality including verification and payment method selection
 */
@Parcelize
internal data class LinkInlineState(
    /**
     * Current verification state representing the different stages in the verification process.
     */
    val verificationState: VerificationState,
) : Parcelable

/**
 * Represents the different states of the verification process
 */
internal sealed class VerificationState : Parcelable {
    /**
     * Initial state while waiting for account information
     */
    @Parcelize
    internal object Loading : VerificationState()

    /**
     * Verification is required and the UI should show the verification form
     */
    @Parcelize
    internal data class Render2FA(
        val viewState: VerificationViewState,
        val linkConfiguration: LinkConfiguration
    ) : VerificationState()

    /**
     * Verification is completed or not needed, showing the normal Link button
     */
    @Parcelize
    internal object RenderButton : VerificationState()
}

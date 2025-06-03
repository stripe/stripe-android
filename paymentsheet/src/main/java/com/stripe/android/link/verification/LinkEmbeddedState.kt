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
     * Current verification state. Null when verification is not needed or completed.
     */
    val verificationState: VerificationViewState? = null,

    /**
     * Preserved Link payment method selection for re-selection after switching payment methods
     */
    val preservedPaymentMethod: LinkPaymentMethod? = null,
) : Parcelable

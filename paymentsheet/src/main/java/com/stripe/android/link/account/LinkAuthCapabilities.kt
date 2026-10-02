package com.stripe.android.link.account

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.VerificationType

/**
 * The verification types this SDK can drive natively. This is the single definition used both to
 * declare `supported_verification_types` to the backend and to select factors in the auth flow, so
 * we never declare a type we can't complete.
 */
internal object LinkAuthCapabilities {
    fun supportedVerificationTypes(): List<VerificationType> {
        return if (FeatureFlags.linkEmailOtpAndMfa.isEnabled) {
            listOf(VerificationType.SMS, VerificationType.EMAIL)
        } else {
            listOf(VerificationType.SMS)
        }
    }
}

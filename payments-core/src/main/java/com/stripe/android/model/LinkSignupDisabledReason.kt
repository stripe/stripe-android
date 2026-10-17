package com.stripe.android.model

import androidx.annotation.RestrictTo

/**
 * Reasons why Link signup may be disabled.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkSignupDisabledReason(val value: String) {
    /**
     * The card funding source is not supported.
     */
    LinkCardNotSupported("link_card_not_supported"),

    /**
     * Link signup is disabled in Elements session. Consult backend logs for more info.
     */
    DisabledInElementsSession("disabled_in_elements_session"),

    /**
     * Link signup opt-in feature is enabled, but the merchant didn't provide an email address
     * via the customer or billing details.
     */
    SignupOptInFeatureNoEmailProvided("signup_opt_in_feature_no_email_provided"),

    /**
     * Attestation is requested, but isn't supported on this device.
     */
    AttestationIssues("attestation_issues"),

    /**
     * The customer has used Link before in this app.
     */
    LinkUsedBefore("link_used_before")
}

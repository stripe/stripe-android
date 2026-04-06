package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable model representing a response from the `/v1/crypto/internal/start_identity_verification`.
 */
@Serializable
internal class StartIdentityVerificationResponse(
    /**
     * The identifier for the resulting identity session
     */
    val id: String,
    /**
     * The hosted Identity Page for redirecting users for Hosted Onramp
     */
    val url: String,
    /** Used to authenticate the mobile Identity SDK.
     * - NOTE: Present only if `is_mobile` was `true` in the request. `nil` otherwise.
     */
    @SerialName("ephemeral_key")
    val ephemeralKey: String?
)

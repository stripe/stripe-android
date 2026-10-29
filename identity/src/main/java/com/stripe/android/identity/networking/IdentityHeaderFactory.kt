package com.stripe.android.identity.networking

import com.stripe.android.core.networking.HEADER_CONTENT_TYPE
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.core.networking.StripeRequest

/**
 * Factory to to create headers for Identity requests.
 * Encapsulates a [RequestHeadersFactory.BaseApiHeadersFactory] instance to mutable ephemeral key
 * attached each time.
 */
internal object IdentityHeaderFactory {
    // Mutable internal instances to change the ephemeralKeys attached to header.
    private var ephemeralKey: String? = null
        set(value) {
            field = value
            apiOptions = RequestCredentials(
                apiKey = requireNotNull(value),
                stripeAccount = null,
            )
        }
    private var apiOptions: RequestCredentials? = null

    private val baseApiHeaderFactory = object : RequestHeadersFactory.BaseApiHeadersFactory(
        optionsProvider = { requireNotNull(apiOptions) },
        apiVersion = IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER,
        idempotencyKey = null,
    ) {
        override var postHeaders = mapOf(
            HEADER_CONTENT_TYPE to StripeRequest.MimeType.Form.toString()
        )
    }

    /**
     * Create header with [ephemeralKey] attached as Bearer token.
     */
    fun createHeaderWithEphemeralKey(ephemeralKey: String): Map<String, String> {
        this.ephemeralKey = ephemeralKey
        return baseApiHeaderFactory.create()
    }

    /**
     * Create post header with multipart/form-data.
     */
    fun createPostHeader() =
        baseApiHeaderFactory.createPostHeader()
}

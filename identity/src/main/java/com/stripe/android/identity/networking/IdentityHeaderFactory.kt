package com.stripe.android.identity.networking

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.HEADER_CONTENT_TYPE
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
            apiOptions = ApiRequest.Options(
                apiKey = requireNotNull(value),
                stripeAccount = null,
                idempotencyKey = null
            )
        }
    private var apiOptions: ApiRequest.Options? = null

    private val baseApiHeaderFactory = object : RequestHeadersFactory.BaseApiHeadersFactory(
        optionsProvider = { requireNotNull(apiOptions) },
        apiVersion = IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER
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

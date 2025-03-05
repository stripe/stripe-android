package com.stripe.android.identity.networking

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.identity.networking.models.VerificationPage

/**
 * Repository to access Stripe backend for Identity.
 */
internal interface IdentityRepository {
    @Throws(
        APIConnectionException::class,
        APIException::class
    )
    suspend fun retrieveVerificationPage(
        id: String,
        ephemeralKey: String
    ): VerificationPage

    /**
     * Fire and forget the analytics request, log an error if exception happens.
     */
    suspend fun sendAnalyticsRequest(analyticsRequestV2: AnalyticsRequestV2)
}

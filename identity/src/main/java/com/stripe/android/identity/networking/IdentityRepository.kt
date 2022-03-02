package com.stripe.android.identity.networking

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData

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

    @Throws(
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postVerificationPageData(
        id: String,
        ephemeralKey: String,
        collectedDataParam: CollectedDataParam
    ): VerificationPageData

    @Throws(
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postVerificationPageSubmit(
        id: String,
        ephemeralKey: String
    ): VerificationPageData
}

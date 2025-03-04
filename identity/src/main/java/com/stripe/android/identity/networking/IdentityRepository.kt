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

//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun postVerificationPageData(
//        id: String,
//        ephemeralKey: String,
//        collectedDataParam: CollectedDataParam,
//        clearDataParam: ClearDataParam
//    ): VerificationPageData
//
//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun postVerificationPageSubmit(
//        id: String,
//        ephemeralKey: String
//    ): VerificationPageData
//
//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun verifyTestVerificationSession(
//        id: String,
//        ephemeralKey: String,
//        simulateDelay: Boolean
//    ): VerificationPageData
//
//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun unverifyTestVerificationSession(
//        id: String,
//        ephemeralKey: String,
//        simulateDelay: Boolean
//    ): VerificationPageData
//
//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun generatePhoneOtp(
//        id: String,
//        ephemeralKey: String
//    ): VerificationPageData
//
//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun cannotVerifyPhoneOtp(
//        id: String,
//        ephemeralKey: String
//    ): VerificationPageData

//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun uploadImage(
//        verificationId: String,
//        ephemeralKey: String,
//        imageFile: File,
//        filePurpose: StripeFilePurpose,
//        onSuccessExecutionTimeBlock: (Long) -> Unit = {}
//    ): StripeFile

//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun downloadModel(modelUrl: String): File
//
//    @Throws(
//        APIConnectionException::class,
//        APIException::class
//    )
//    suspend fun downloadFile(fileUrl: String): File

    /**
     * Fire and forget the analytics request, log an error if exception happens.
     */
    suspend fun sendAnalyticsRequest(analyticsRequestV2: AnalyticsRequestV2)
}

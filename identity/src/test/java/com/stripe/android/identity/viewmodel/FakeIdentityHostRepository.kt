package com.stripe.android.identity.viewmodel

import app.cash.turbine.Turbine
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.CompletableDeferred
import java.io.File

internal class FakeIdentityHostRepository : IdentityRepository {
    val dataCalls = Turbine<DataCall>()
    val submitCalls = Turbine<SubmitCall>()
    val unexpectedCalls = Turbine<String>()
    val dataResponse = CompletableDeferred<VerificationPageData>()
    val submitResponse = CompletableDeferred<VerificationPageData>()

    override suspend fun postVerificationPageData(
        id: String,
        ephemeralKey: String,
        collectedDataParam: CollectedDataParam,
        clearDataParam: ClearDataParam
    ): VerificationPageData {
        dataCalls.add(DataCall(id, ephemeralKey, collectedDataParam, clearDataParam))
        return dataResponse.await()
    }

    override suspend fun postVerificationPageSubmit(id: String, ephemeralKey: String): VerificationPageData {
        submitCalls.add(SubmitCall(id, ephemeralKey))
        return submitResponse.await()
    }

    override suspend fun retrieveVerificationPage(id: String, ephemeralKey: String): VerificationPage =
        unexpected("retrieveVerificationPage")

    override suspend fun verifyTestVerificationSession(
        id: String,
        ephemeralKey: String,
        simulateDelay: Boolean
    ): VerificationPageData = unexpected("verifyTestVerificationSession")

    override suspend fun unverifyTestVerificationSession(
        id: String,
        ephemeralKey: String,
        simulateDelay: Boolean
    ): VerificationPageData = unexpected("unverifyTestVerificationSession")

    override suspend fun generatePhoneOtp(id: String, ephemeralKey: String): VerificationPageData =
        unexpected("generatePhoneOtp")

    override suspend fun cannotVerifyPhoneOtp(id: String, ephemeralKey: String): VerificationPageData =
        unexpected("cannotVerifyPhoneOtp")

    override suspend fun uploadImage(
        verificationId: String,
        ephemeralKey: String,
        imageFile: File,
        filePurpose: StripeFilePurpose,
        onSuccessExecutionTimeBlock: (Long) -> Unit
    ): StripeFile = unexpected("uploadImage")

    override suspend fun downloadModel(modelUrl: String): File = unexpected("downloadModel")

    override suspend fun downloadFile(fileUrl: String): File = unexpected("downloadFile")

    override suspend fun sendAnalyticsRequest(analyticsRequestV2: AnalyticsRequestV2): Unit =
        unexpected("sendAnalyticsRequest")

    private fun unexpected(name: String): Nothing {
        unexpectedCalls.add(name)
        error("Unexpected Identity repository call: $name")
    }

    fun ensureAllEventsConsumed() {
        dataCalls.ensureAllEventsConsumed()
        submitCalls.ensureAllEventsConsumed()
        unexpectedCalls.ensureAllEventsConsumed()
    }

    data class DataCall(
        val id: String,
        val ephemeralKey: String,
        val collectedData: CollectedDataParam,
        val clearData: ClearDataParam
    )

    data class SubmitCall(val id: String, val ephemeralKey: String)
}

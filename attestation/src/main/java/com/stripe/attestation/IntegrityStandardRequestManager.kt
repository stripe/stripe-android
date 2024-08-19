package com.stripe.attestation

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.stripe.android.core.networking.StripeRequest
import com.stripe.attestation.domain.BuildRequestIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntegrityStandardRequestManager(
    // TODO figure out GCP.
    private val cloudProjectNumber: Long = 123456789L,
    private val buildRequestIdentifier: BuildRequestIdentifier = BuildRequestIdentifier(),
    appContext: Context
) {

    private val standardIntegrityManager by lazy { IntegrityManagerFactory.createStandard(appContext) }
    private lateinit var integrityTokenProvider: StandardIntegrityTokenProvider

    // Prepare integrity token.
    // Can be called once in a while to keep internal state fresh.
    suspend fun prepare() = suspendCancellableCoroutine<Result<Unit>> { continuation ->
        runCatching {
            standardIntegrityManager.prepareIntegrityToken(
                PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(cloudProjectNumber)
                    .build()
            )
                .addOnSuccessListener {
                    Log.e("Integrity", "Prepared integrity token successfully")
                    integrityTokenProvider = it
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener {
                    Log.e("Integrity", "Failed to prepare integrity token", it)
                    continuation.resume(Result.failure(it))
                }
        }.onFailure {
            Log.e("Integrity", "Failed to prepare integrity token", it)
            continuation.resume(Result.failure(it))
        }

    }

    suspend fun requestToken(
        requestIdentifier: String,
    ): Result<String> {
        Log.d("Integrity", "Requesting integrity token request $requestIdentifier")
        val requestHash = buildRequestIdentifier(requestIdentifier)
        Log.d("Integrity", "Hashed request identifier $requestHash")
        return request(requestHash)
    }

    suspend fun requestToken(
        request: StripeRequest,
    ): Result<String> {
        Log.d("Integrity", "Requesting integrity token request ${request.url}")
        val requestHash = buildRequestIdentifier(request)
        Log.d("Integrity", "Hashed request identifier $requestHash")
        return request(requestHash)
    }

    private suspend fun request(
        requestHash: String?,
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        runCatching {
            integrityTokenProvider.request(
                StandardIntegrityTokenRequest.builder()
                    .setRequestHash(requestHash)
                    .build()
            )
                .addOnSuccessListener { response: StandardIntegrityToken ->
                    val token = response.token()
                    Log.d("Integrity", "Received integrity token $token")
                    continuation.resume(Result.success(token))
                }
                .addOnFailureListener { exception: Exception ->
                    Log.e("Integrity", "Failed to request integrity token", exception)
                    continuation.resume(Result.failure(exception))
                }
        }.onFailure {
            Log.e("Integrity", "Failed to request integrity token", it)
            continuation.resume(Result.failure(it))
        }
    }
}
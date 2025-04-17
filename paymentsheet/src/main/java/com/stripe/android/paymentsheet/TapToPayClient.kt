package com.stripe.android.paymentsheet

import android.Manifest
import androidx.annotation.RequiresPermission
import com.stripe.Stripe
import com.stripe.android.core.exception.StripeException
import com.stripe.cots.aidlservice.CotsContactlessResult
import com.stripe.cots.aidlservice.CotsReader
import com.stripe.cots.aidlservice.CotsService
import com.stripe.cots.common.CotsClientInterface
import com.stripe.cots.common.CotsCollectionParameters
import com.stripe.currency.Amount
import com.stripe.jvmcore.restclient.RestConfig
import com.stripe.model.terminal.ConnectionToken
import com.stripe.net.RequestOptions
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.RoutingPriority
import com.stripe.stripeterminal.internal.common.api.ApiClient
import com.stripe.terminal.appinfo.ApplicationInformationProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TapToPayClient @Inject constructor(
    private val cotsClient: CotsClientInterface,
    private val appInfoProvider: ApplicationInformationProvider,
    private val apiClient: ApiClient,
    private val tokenProvider: ConnectionTokenProvider,
) : TapToPayReaderListener {
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun activate() {
        val connectionConfiguration = ConnectionConfiguration.TapToPayConnectionConfiguration(
            locationId = locationId,
            autoReconnectOnUnexpectedDisconnect = false,
            tapToPayReaderListener = this,
        )

        val token = tokenProvider.fetchConnectionToken()
        val response = apiClient.activateReader(
            reader = Reader(
                DeviceType.TAP_TO_PAY_DEVICE,
                isSimulated = false,
                rawSerialNumber = "12332523552323",
            ),
            token = token,
            connectionConfig = connectionConfiguration,
            restConfig = RestConfig.withMaxAttempts(2)
        )

        val applicationInfo = appInfoProvider.get()
        val cotsReader = cotsClient.activate(
            sessionToken = response.stripeSessionToken.orEmpty(),
            readerRpcSessionToken = response.readerRpcSessionToken.orEmpty(),
            sdkRpcSessionToken = response.sdkRpcSessionToken.orEmpty(),
            clientVersion = applicationInfo.clientVersion,
            clientType = applicationInfo.clientType,
            deviceUuid = applicationInfo.deviceUuid.value,
            countryCode = "CA",
        )

        checkAndThrowCotsError(cotsReader)
    }

    suspend fun collect(amount: Long, currencyCode: String): Result<CollectResult> {
        val result = cotsClient.collectPayment(
            CotsCollectionParameters(
                amount = Amount(amount, currencyCode),
                routingPriority = RoutingPriority.DOMESTIC,
            )
        )

        return if (
            result?.outcome == CotsContactlessResult.ContactlessOutcome.APPROVED
            || result?.outcome == CotsContactlessResult.ContactlessOutcome.PROCEED_ONLINE
            || result?.outcome == CotsContactlessResult.ContactlessOutcome.PIN_COLLECTED
        ) {
            Result.success(
                CollectResult(
                    emvData = result.emvBlob,
                    encryptedTrack2 = result.encryptedTrack2,
                )
            )
        } else {
            Result.failure(Exception("Error: ${result?.outcome?.name ?: "UNKNOWN"}, Message: ${result?.userErrorMessage}"))
        }
    }

    private fun checkAndThrowCotsError(cotsReader: CotsReader?) {
        cotsReader ?: throw StripeException.create(IllegalStateException("Null cotsReader"))

        when (val code = cotsReader.errorCode) {
            CotsReader.CotsError.NONE -> return
            else -> throw StripeException.create(IllegalStateException(code.name))
        }
    }

    data class CollectResult(
        val emvData: String,
        val encryptedTrack2: String,
    )
}

private suspend inline fun ConnectionTokenProvider.fetchConnectionToken(): String {
    return suspendCancellableCoroutine { continuation ->
        fetchConnectionToken(
            object : ConnectionTokenCallback {
                override fun onSuccess(token: String) {
                    continuation.resume(token)
                }

                override fun onFailure(e: ConnectionTokenException) {
                    continuation.resumeWithException(e)
                }
            }
        )
    }
}

object StripeApiTokenProvider : ConnectionTokenProvider {
    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        Stripe.apiKey = "secret_key"

        callback.onSuccess(ConnectionToken.create(mapOf(), requestOptions).secret)
    }
}

private val requestOptions: RequestOptions = RequestOptions.builder()
    .build()

private const val locationId = "tml_here"
package com.stripe.android.paymentsheet

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.stripe.Stripe
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.cots.aidlservice.CotsContactlessResult
import com.stripe.cots.aidlservice.CotsReader
import com.stripe.cots.common.CotsClientInterface
import com.stripe.cots.common.CotsCollectionParameters
import com.stripe.cots.common.CotsCollectionType
import com.stripe.currency.Amount
import com.stripe.hardware.emv.ApplicationId
import com.stripe.hardware.emv.InterfaceType
import com.stripe.hardware.emv.SourceType
import com.stripe.hardware.emv.TlvMap
import com.stripe.hardware.emv.TlvMap.Companion.toTlvMap
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
    suspend fun activate(): String {
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
        val stripeSessionToken = response.stripeSessionToken.orEmpty()
        val cotsReader = cotsClient.activate(
            sessionToken = stripeSessionToken,
            readerRpcSessionToken = response.readerRpcSessionToken.orEmpty(),
            sdkRpcSessionToken = response.sdkRpcSessionToken.orEmpty(),
            clientVersion = applicationInfo.clientVersion,
            clientType = applicationInfo.clientType,
            deviceUuid = applicationInfo.deviceUuid.value,
            countryCode = "CA",
            accountId = "",
        )

        checkAndThrowCotsError(cotsReader)

        return stripeSessionToken
    }

    suspend fun collect(
        sessionToken: String,
        amount: Long,
        currencyCode: String
    ): Result<PaymentMethodCreateParams> {
        val result = cotsClient.collectPayment(
            CotsCollectionParameters(
                amount = Amount(amount, currencyCode),
                routingPriority = RoutingPriority.DOMESTIC,
                collectionType = CotsCollectionType.SALE,
            )
        )

        return if (
            result?.outcome == CotsContactlessResult.ContactlessOutcome.APPROVED
            || result?.outcome == CotsContactlessResult.ContactlessOutcome.PROCEED_ONLINE
            || result?.outcome == CotsContactlessResult.ContactlessOutcome.PIN_COLLECTED
        ) {
            Log.d("CARD_DETAILS", result.emvBlob.toTlvMap()?.let { Details(it).toString() } ?: "NONE")

            Result.success(
                PaymentMethodCreateParams.create(
                    cardPresent = PaymentMethodCreateParams.CardPresent(
                        emv = result.emvBlob,
                        encryptedTrack2 = result.encryptedTrack2,
                        cryptogram = result.cryptogram,
                        latitude = "42.991",
                        longitude = "80.628",
                        posSessionToken = sessionToken,
                    )
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

private data class Details private constructor(
    val applicationId: ApplicationId?,
    val applicationName: String,
    val automaticallySelectApplication: Boolean,
    val dedicatedFileName: String,
    val interfaceType: InterfaceType?,
    val sourceType: SourceType?,
    val cardholderVerificationMethod: String?,
    val cardholderName: String?,
    val expMonth: String?,
    val expYear: String?,
) {
    constructor(map: TlvMap): this(
        applicationId = map.applicationId,
        applicationName = map.applicationName,
        dedicatedFileName = map.dedicatedFileName,
        automaticallySelectApplication = map.automaticallySelectApplication,
        interfaceType = map.interfaceType,
        sourceType = map.sourceType,
        cardholderVerificationMethod = map.cardholderVerificationMethod,
        cardholderName = map.cardholderName,
        expMonth = map.expMonthAndYear?.first,
        expYear = map.expMonthAndYear?.second,
    )
}

private val requestOptions: RequestOptions = RequestOptions.builder()
    .build()

private const val locationId = "tml_FvEtUAnR8uvNbk"
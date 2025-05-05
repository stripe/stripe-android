package com.stripe.android.paymentsheet

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.cots.aidlservice.CotsContactlessResult
import com.stripe.cots.aidlservice.CotsReader
import com.stripe.cots.common.CotsClientInterface
import com.stripe.cots.common.CotsCollectionParameters
import com.stripe.currency.Amount
import com.stripe.hardware.emv.ApplicationId
import com.stripe.hardware.emv.InterfaceType
import com.stripe.hardware.emv.SourceType
import com.stripe.hardware.emv.TlvMap
import com.stripe.hardware.emv.TlvMap.Companion.toTlvMap
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.models.RoutingPriority
import com.stripe.terminal.appinfo.ApplicationInformationProvider
import javax.inject.Inject

class TapToPayClient @Inject constructor(
    private val cotsClient: CotsClientInterface,
    private val appInfoProvider: ApplicationInformationProvider,
) : TapToPayReaderListener {
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun activate(): String {
        val applicationInfo = appInfoProvider.get()
        val cotsReader = cotsClient.activate(
            sessionToken = "session_token",
            readerRpcSessionToken = "rpc_session_token",
            sdkRpcSessionToken = "sdk_rpc_session_token",
            clientVersion = applicationInfo.clientVersion,
            clientType = applicationInfo.clientType,
            deviceUuid = applicationInfo.deviceUuid.value,
            countryCode = "CA",
        )

        checkAndThrowCotsError(cotsReader)

        return "session_token"
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

object StripeApiTokenProvider : ConnectionTokenProvider {
    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        throw Exception("test")
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
    val first6: String?,
    val last4: String?,
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
        first6 = map.first6,
        last4 = map.last4,
        expMonth = map.expMonthAndYear?.first,
        expYear = map.expMonthAndYear?.second,
    )
}
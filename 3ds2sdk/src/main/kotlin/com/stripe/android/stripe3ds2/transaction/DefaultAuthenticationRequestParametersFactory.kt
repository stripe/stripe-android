package com.stripe.android.stripe3ds2.transaction

import androidx.annotation.VisibleForTesting
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.init.AppInfoRepository
import com.stripe.android.stripe3ds2.init.DeviceDataFactory
import com.stripe.android.stripe3ds2.init.DeviceParamNotAvailableFactory
import com.stripe.android.stripe3ds2.init.SecurityChecker
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.security.DefaultJweEncrypter
import com.stripe.android.stripe3ds2.security.DirectoryServer
import com.stripe.android.stripe3ds2.security.EphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.security.JweEncrypter
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import kotlin.coroutines.CoroutineContext

internal class DefaultAuthenticationRequestParametersFactory internal constructor(
    private val deviceDataFactory: DeviceDataFactory,
    private val deviceParamNotAvailableFactory: DeviceParamNotAvailableFactory,
    private val securityChecker: SecurityChecker,
    private val appInfoRepository: AppInfoRepository,
    private val jweEncrypter: JweEncrypter,
    private val messageVersionRegistry: MessageVersionRegistry,
    private val sdkReferenceNumber: String,
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext
) : AuthenticationRequestParametersFactory {
    /**
     * See "SDK—Device Information - Table 2.7: Device Parameters JSON Structure"
     *
     * @return a JSON string representing the device data in the expected format
     *
     * For example:
     * {
     * "DV":"1.0",
     * "DD":{"C001":"Android","C002":"HTC One_M8"},
     * "DPNA":{"C010":"RE01","C011":"RE 03"},
     * "SW":["SW01","SW04"]
     * }
     */
    @VisibleForTesting
    @Throws(JSONException::class)
    internal suspend fun deviceDataJson(sdkTransactionId: SdkTransactionId): JSONObject {
        return JSONObject()
            .put(KEY_DATA_VERSION, DATA_VERSION)
            .put(KEY_DEVICE_DATA, JSONObject(deviceDataFactory.create(sdkReferenceNumber, sdkTransactionId)))
            .put(
                KEY_DEVICE_PARAM_NOT_AVAILABLE,
                JSONObject(deviceParamNotAvailableFactory.create())
            )
            .put(
                KEY_SECURITY_WARNINGS,
                JSONArray(securityChecker.getWarnings().map { it.id })
            )
    }

    constructor(
        deviceDataFactory: DeviceDataFactory,
        deviceParamNotAvailableFactory: DeviceParamNotAvailableFactory,
        securityChecker: SecurityChecker,
        ephemeralKeyPairGenerator: EphemeralKeyPairGenerator,
        appInfoRepository: AppInfoRepository,
        messageVersionRegistry: MessageVersionRegistry,
        sdkReferenceNumber: String,
        errorReporter: ErrorReporter,
        workContext: CoroutineContext
    ) : this(
        deviceDataFactory,
        deviceParamNotAvailableFactory,
        securityChecker,
        appInfoRepository,
        DefaultJweEncrypter(
            ephemeralKeyPairGenerator,
            errorReporter
        ),
        messageVersionRegistry,
        sdkReferenceNumber,
        errorReporter,
        workContext
    )

    /**
     * @param directoryServerId the DS id
     * @param sdkTransactionId a transaction ID generated by [StripeThreeDs2ServiceImpl]
     * @param sdkPublicKey public key generated by [EphemeralKeyPairGenerator] that will be
     * used for Diffie-Hellman key exchange
     * @return a properly configured [AuthenticationRequestParameters] instance
     */
    override suspend fun create(
        directoryServerId: String,
        directoryServerPublicKey: PublicKey,
        keyId: String?,
        sdkTransactionId: SdkTransactionId,
        sdkPublicKey: PublicKey
    ): AuthenticationRequestParameters = withContext(workContext) {
        val deviceData = runCatching {
            jweEncrypter.encrypt(
                deviceDataJson(sdkTransactionId).toString(),
                directoryServerPublicKey,
                directoryServerId,
                keyId
            )
        }.onFailure {
            errorReporter.reportError(
                RuntimeException(
                    """
                    Failed to encrypt AReq parameters.
                        
                    directoryServerId=$directoryServerId
                    keyId=$keyId
                    sdkTransactionId=$sdkTransactionId
                    """.trimIndent(),
                    it
                )
            )
        }.getOrElse {
            throw SDKRuntimeException(it)
        }

        AuthenticationRequestParameters(
            deviceData = deviceData,
            sdkTransactionId = sdkTransactionId,
            sdkAppId = appInfoRepository.get().sdkAppId,
            sdkReferenceNumber = sdkReferenceNumber,
            sdkEphemeralPublicKey = createPublicJwk(
                sdkPublicKey,
                keyId,
                keyUse = getKeyUse(directoryServerId)
            ).toJSONString(),
            messageVersion = messageVersionRegistry.current
        )
    }

    /**
     * Get the [KeyUse] value for a given directory server ID. If this is an unknown
     * directory server, default to [KeyUse.SIGNATURE].
     *
     * See [DirectoryServer.keyUse] for default values.
     */
    @VisibleForTesting
    internal fun getKeyUse(directoryServerId: String): KeyUse? {
        val directoryServer = DirectoryServer.entries.firstOrNull {
            it.ids.contains(directoryServerId)
        }

        return when {
            directoryServer != null -> directoryServer.keyUse
            else -> KeyUse.SIGNATURE
        }
    }

    internal companion object {
        internal const val KEY_DATA_VERSION = "DV"
        internal const val KEY_DEVICE_DATA = "DD"
        internal const val KEY_DEVICE_PARAM_NOT_AVAILABLE = "DPNA"
        internal const val KEY_SECURITY_WARNINGS = "SW"

        private const val DATA_VERSION = "1.6"

        /**
         * Creates a public JSON Web Key (JWK) [0] from the given [KeyPair] using the
         * Nimbus JOSE + JWT library [1]
         *
         * [0] https://tools.ietf.org/html/rfc7517
         * [1] https://connect2id.com/products/nimbus-jose-jwt/examples/jwk-generation
         */
        @VisibleForTesting
        internal fun createPublicJwk(
            publicKey: PublicKey,
            keyId: String?,
            keyUse: KeyUse?
        ): JWK {
            return ECKey.Builder(Curve.P_256, publicKey as ECPublicKey)
                .keyUse(keyUse)
                .keyID(keyId.takeUnless { it.isNullOrBlank() })
                .build()
                .toPublicJWK()
        }
    }
}

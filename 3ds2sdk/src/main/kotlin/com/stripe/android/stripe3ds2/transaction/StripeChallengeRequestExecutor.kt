package com.stripe.android.stripe3ds2.transaction

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JOSEObject
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.security.DiffieHellmanKeyGenerator
import com.stripe.android.stripe3ds2.security.EcKeyFactory
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.security.StripeDiffieHellmanKeyGenerator
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.transactions.ProtocolError
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import kotlin.coroutines.CoroutineContext

internal class StripeChallengeRequestExecutor internal constructor(
    private val messageTransformer: MessageTransformer,
    private val sdkReferenceId: String,
    private val sdkPrivateKey: PrivateKey,
    private val acsPublicKey: ECPublicKey,
    acsUrl: String,
    private val errorReporter: ErrorReporter,
    private val dhKeyGenerator: DiffieHellmanKeyGenerator,
    private val workContext: CoroutineContext,
    private val httpClient: HttpClient = StripeHttpClient(
        acsUrl,
        errorReporter = errorReporter,
        workContext = workContext
    ),
    private val creqExecutorConfig: ChallengeRequestExecutor.Config,
    responseProcessorFactory: ChallengeResponseProcessorFactory =
        ChallengeResponseProcessorFactory.Default(
            messageTransformer,
            errorReporter,
            creqExecutorConfig
        )
) : ChallengeRequestExecutor {
    private val secretKey = generateSecretKey()
    private val responseProcessor =
        responseProcessorFactory.create(secretKey)

    /**
     * Executes the challenge request (CRes)/response (CRes) lifecycle:
     * 1. generate a Diffie-Hellman secret key
     * 2. encrypt the CReq payload with the key
     * 3. make a challenge request to the ACS with [StripeHttpClient]
     * 4. pass the encrypted response body to [ChallengeResponseProcessor] for processing
     */
    override suspend fun execute(
        creqData: ChallengeRequestData
    ): ChallengeRequestResult {
        return withTimeoutOrNull(TIMEOUT) {
            runCatching {
                httpClient.doPostRequest(
                    getRequestBody(creqData.toJson()),
                    JOSEObject.MIME_TYPE_COMPACT
                )
            }.onFailure {
                errorReporter.reportError(it)
            }.fold(
                onSuccess = { response ->
                    responseProcessor.process(creqData, response)
                },
                onFailure = { error ->
                    ChallengeRequestResult.RuntimeError(error)
                }
            )
        } ?: createTimeoutResult(creqData)
    }

    @Throws(JSONException::class, JOSEException::class)
    private fun getRequestBody(payload: JSONObject): String {
        return messageTransformer.encrypt(payload, secretKey)
    }

    private fun generateSecretKey(): SecretKey {
        return dhKeyGenerator.generate(
            acsPublicKey,
            sdkPrivateKey as ECPrivateKey,
            sdkReferenceId
        )
    }

    internal class Factory(
        private val config: ChallengeRequestExecutor.Config
    ) : ChallengeRequestExecutor.Factory {
        override fun create(
            errorReporter: ErrorReporter,
            workContext: CoroutineContext
        ): ChallengeRequestExecutor {
            val ecKeyFactory = EcKeyFactory(errorReporter)
            return StripeChallengeRequestExecutor(
                config.messageTransformer,
                config.sdkReferenceId,
                ecKeyFactory.createPrivate(config.keys.sdkPrivateKeyEncoded),
                ecKeyFactory.createPublic(config.keys.acsPublicKeyEncoded),
                config.acsUrl,
                errorReporter,
                StripeDiffieHellmanKeyGenerator(errorReporter),
                workContext = workContext,
                creqExecutorConfig = config
            )
        }
    }

    internal companion object {
        val TIMEOUT = TimeUnit.SECONDS.toMillis(10)

        private fun createTimeoutResult(
            creqData: ChallengeRequestData
        ) = ChallengeRequestResult.Timeout(
            ErrorData(
                sdkTransId = creqData.sdkTransId,
                messageVersion = creqData.messageVersion,
                acsTransId = creqData.acsTransId,
                serverTransId = creqData.threeDsServerTransId,
                errorCode = ProtocolError.TransactionTimedout.code.toString(),
                errorDescription = ProtocolError.TransactionTimedout.description,
                errorComponent = ErrorData.ErrorComponent.ThreeDsSdk,
                errorMessageType = ChallengeRequestData.MESSAGE_TYPE,
                errorDetail = "Challenge request timed-out"
            )
        )
    }
}

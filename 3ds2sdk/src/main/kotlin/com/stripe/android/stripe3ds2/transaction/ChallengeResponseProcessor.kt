package com.stripe.android.stripe3ds2.transaction

import androidx.annotation.VisibleForTesting
import com.nimbusds.jose.JOSEException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseParseException
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.transactions.ProtocolError
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import javax.crypto.SecretKey

/**
 * Response for transforming an encrypted CRes into a hydrated [ChallengeResponseData] object,
 * or an [ErrorData] object if processing fails.
 */
internal interface ChallengeResponseProcessor {
    suspend fun process(
        creqData: ChallengeRequestData,
        response: HttpResponse
    ): ChallengeRequestResult

    class Default internal constructor(
        private val messageTransformer: MessageTransformer,
        private val secretKey: SecretKey,
        private val errorReporter: ErrorReporter,
        private val creqExecutorConfig: ChallengeRequestExecutor.Config
    ) : ChallengeResponseProcessor {
        override suspend fun process(
            creqData: ChallengeRequestData,
            response: HttpResponse
        ): ChallengeRequestResult {
            return if (response.isJsonContentType) {
                val payload = JSONObject(response.content)
                if (ErrorData.isErrorMessage(payload)) {
                    ChallengeRequestResult.ProtocolError(
                        ErrorData.fromJson(payload)
                    )
                } else {
                    ChallengeRequestResult.RuntimeError(
                        IllegalArgumentException("Received a JSON response that was not an Error message.")
                    )
                }
            } else {
                runCatching {
                    getResponsePayload(response.content)
                }.onFailure {
                    errorReporter.reportError(
                        java.lang.RuntimeException(
                            """
                            Failed to process challenge response.

                            CReq = ${creqData.sanitize()}
                            """.trimIndent(),
                            it
                        )
                    )
                }.fold(
                    onSuccess = {
                        processPayload(creqData, it)
                    },
                    onFailure = {
                        val protocolError = ProtocolError.DataDecryptionFailure
                        ChallengeRequestResult.ProtocolError(
                            createErrorData(
                                creqData,
                                protocolError.code,
                                protocolError.description,
                                it.message.orEmpty()
                            )
                        )
                    }
                )
            }
        }

        @VisibleForTesting
        fun processPayload(
            creqData: ChallengeRequestData,
            payload: JSONObject
        ): ChallengeRequestResult {
            return if (ErrorData.isErrorMessage(payload)) {
                ChallengeRequestResult.ProtocolError(ErrorData.fromJson(payload))
            } else {
                runCatching {
                    ChallengeResponseData.fromJson(payload)
                }.fold(
                    onSuccess = { cresData ->
                        if (!isValidChallengeResponse(creqData, cresData)) {
                            val protocolError = ProtocolError.InvalidTransactionId
                            ChallengeRequestResult.ProtocolError(
                                createErrorData(
                                    creqData,
                                    protocolError.code,
                                    protocolError.description,
                                    "The Transaction ID received was invalid."
                                )
                            )
                        } else if (!isMessageVersionCorrect(creqData, cresData)) {
                            ChallengeRequestResult.ProtocolError(
                                createErrorData(
                                    creqData,
                                    ProtocolError.UnsupportedMessageVersion.code,
                                    ProtocolError.UnsupportedMessageVersion.description,
                                    creqData.messageVersion
                                )
                            )
                        } else {
                            ChallengeRequestResult.Success(creqData, cresData, creqExecutorConfig)
                        }
                    },
                    onFailure = { throwable ->
                        when (throwable) {
                            is ChallengeResponseParseException -> {
                                ChallengeRequestResult.ProtocolError(
                                    createErrorData(
                                        creqData,
                                        throwable.code,
                                        throwable.description,
                                        throwable.detail
                                    )
                                )
                            }
                            else -> {
                                ChallengeRequestResult.RuntimeError(throwable)
                            }
                        }
                    }
                )
            }
        }

        private fun isValidChallengeResponse(
            creqData: ChallengeRequestData,
            cresData: ChallengeResponseData
        ): Boolean {
            return creqData.sdkTransId == cresData.sdkTransId &&
                creqData.threeDsServerTransId == cresData.serverTransId &&
                creqData.acsTransId == cresData.acsTransId
        }

        private fun isMessageVersionCorrect(
            creqData: ChallengeRequestData,
            cresData: ChallengeResponseData
        ): Boolean {
            return creqData.messageVersion == cresData.messageVersion
        }

        @Throws(
            ParseException::class, JOSEException::class, JSONException::class,
            ChallengeResponseParseException::class
        )
        private fun getResponsePayload(responseBody: String): JSONObject {
            return messageTransformer.decrypt(responseBody, secretKey)
        }

        private fun createErrorData(
            creqData: ChallengeRequestData,
            code: Int,
            description: String,
            detail: String
        ): ErrorData {
            return ErrorData(
                errorCode = code.toString(),
                errorDescription = description,
                errorDetail = detail,
                errorMessageType = ChallengeResponseData.MESSAGE_TYPE,
                errorComponent = ErrorData.ErrorComponent.ThreeDsSdk,
                acsTransId = creqData.acsTransId,
                sdkTransId = creqData.sdkTransId,
                serverTransId = creqData.threeDsServerTransId,
                messageVersion = creqData.messageVersion
            )
        }
    }
}

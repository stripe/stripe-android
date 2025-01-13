package com.stripe.android.stripe3ds2.transaction

import android.app.Application
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.DefaultErrorReporter
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.observability.Stripe3ds2ErrorReporterConfig
import com.stripe.android.stripe3ds2.security.DefaultMessageTransformer
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.views.ChallengeViewArgs
import java.security.cert.X509Certificate
import kotlin.coroutines.CoroutineContext

interface InitChallengeRepository {
    suspend fun startChallenge(
        args: InitChallengeArgs
    ): InitChallengeResult
}

internal class DefaultInitChallengeRepository internal constructor(
    private val sdkTransactionId: SdkTransactionId,
    private val messageVersionRegistry: MessageVersionRegistry,
    private val jwsValidator: JwsValidator,
    private val messageTransformer: MessageTransformer,
    private val acsDataParser: AcsDataParser,
    private val challengeRequestResultRepository: ChallengeRequestResultRepository,
    private val errorRequestExecutorFactory: ErrorRequestExecutor.Factory,
    private val uiCustomization: StripeUiCustomization,
    private val errorReporter: ErrorReporter,
    private val logger: Logger
) : InitChallengeRepository {

    /**
     * Make the initial challenge request and return a [InitChallengeResult] representing the
     * result. If successful, will return [InitChallengeResult.Start] to start the challenge UI;
     * otherwise, will return a [InitChallengeResult.End] that indicates the challenge should end.
     */
    override suspend fun startChallenge(
        args: InitChallengeArgs
    ): InitChallengeResult {
        logger.info("Make initial challenge request.")

        return runCatching {
            // will throw exception if acsSignedContent fails verification
            val (acsUrl, acsEphemPubKey) = acsDataParser.parse(
                jwsValidator.getPayload(
                    requireNotNull(args.challengeParameters.acsSignedContent)
                )
            )

            val creqData = createCreqData(sdkTransactionId, args.challengeParameters)

            val errorRequestExecutor = errorRequestExecutorFactory.create(acsUrl, errorReporter)

            val creqExecutorConfig = ChallengeRequestExecutor.Config(
                messageTransformer,
                args.sdkReferenceNumber,
                creqData,
                acsUrl,
                ChallengeRequestExecutor.Config.Keys(
                    args.sdkKeyPair.private.encoded,
                    acsEphemPubKey.encoded
                )
            )

            val challengeRequestResult = challengeRequestResultRepository.get(
                creqExecutorConfig,
                creqData
            )

            when (challengeRequestResult) {
                is ChallengeRequestResult.Success -> {
                    InitChallengeResult.Start(
                        ChallengeViewArgs(
                            challengeRequestResult.cresData,
                            challengeRequestResult.creqData,
                            uiCustomization,
                            creqExecutorConfig,
                            StripeChallengeRequestExecutor.Factory(creqExecutorConfig),
                            args.timeoutMins,
                            args.intentData
                        )
                    )
                }
                is ChallengeRequestResult.ProtocolError -> {
                    if (challengeRequestResult.data.errorComponent == ErrorData.ErrorComponent.ThreeDsSdk) {
                        errorRequestExecutor.executeAsync(challengeRequestResult.data)
                    }

                    InitChallengeResult.End(
                        ChallengeResult.ProtocolError(
                            challengeRequestResult.data,
                            null,
                            args.intentData
                        )
                    )
                }
                is ChallengeRequestResult.Timeout -> {
                    errorRequestExecutor.executeAsync(challengeRequestResult.data)

                    InitChallengeResult.End(
                        ChallengeResult.Timeout(
                            null,
                            null,
                            args.intentData
                        )
                    )
                }
                is ChallengeRequestResult.RuntimeError -> {
                    InitChallengeResult.End(
                        ChallengeResult.RuntimeError(
                            challengeRequestResult.throwable,
                            null,
                            args.intentData
                        )
                    )
                }
            }
        }.getOrElse {
            errorReporter.reportError(it)
            logger.error("Exception during initial challenge request.", it)

            InitChallengeResult.End(
                ChallengeResult.RuntimeError(
                    it,
                    null,
                    args.intentData
                )
            )
        }
    }

    private fun createCreqData(
        sdkTransactionId: SdkTransactionId,
        challengeParameters: ChallengeParameters
    ) = ChallengeRequestData(
        acsTransId = requireNotNull(challengeParameters.acsTransactionId),
        threeDsServerTransId = requireNotNull(challengeParameters.threeDsServerTransactionId),
        sdkTransId = sdkTransactionId,
        messageVersion = messageVersionRegistry.current,
        threeDSRequestorAppURL = challengeParameters.threeDSRequestorAppURL
    )
}

class InitChallengeRepositoryFactory(
    private val application: Application,
    private val isLiveMode: Boolean,
    private val sdkTransactionId: SdkTransactionId,
    private val uiCustomization: StripeUiCustomization,
    private val rootCerts: List<X509Certificate>,
    private val enableLogging: Boolean,
    private val workContext: CoroutineContext,
) {
    fun create(): InitChallengeRepository {
        val logger = Logger.get(enableLogging)
        val errorReporter = DefaultErrorReporter(
            application,
            Stripe3ds2ErrorReporterConfig(sdkTransactionId),
            workContext,
            logger
        )
        return DefaultInitChallengeRepository(
            sdkTransactionId,
            MessageVersionRegistry(),
            DefaultJwsValidator(
                isLiveMode,
                rootCerts,
                errorReporter
            ),
            DefaultMessageTransformer(isLiveMode),
            DefaultAcsDataParser(errorReporter),
            DefaultChallengeRequestResultRepository(errorReporter, workContext),
            StripeErrorRequestExecutor.Factory(workContext),
            uiCustomization,
            errorReporter,
            logger
        )
    }
}

package com.stripe.android.payments.core.authentication.threeds2

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.StripePaymentController
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2Fingerprint
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.WeakSetInjectorRegistry
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.ChallengeParameters
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.InitChallengeArgs
import com.stripe.android.stripe3ds2.transaction.InitChallengeRepository
import com.stripe.android.stripe3ds2.transaction.InitChallengeRepositoryFactory
import com.stripe.android.stripe3ds2.transaction.IntentData
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class Stripe3ds2TransactionViewModel(
    private val args: Stripe3ds2TransactionContract.Args,
    private val initChallengeRepository: InitChallengeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), Injectable {

    @Inject
    lateinit var stripeRepository: StripeRepository

    @Inject
    lateinit var analyticsRequestExecutor: AnalyticsRequestExecutor

    @Inject
    lateinit var analyticsRequestFactory: AnalyticsRequestFactory

    @Inject
    lateinit var messageVersionRegistry: MessageVersionRegistry

    @Inject
    lateinit var threeDs2Service: StripeThreeDs2Service

    @Inject
    lateinit var challengeResultProcessor: Stripe3ds2ChallengeResultProcessor

    @Inject
    @IOContext
    lateinit var workContext: CoroutineContext

    init {
        WeakSetInjectorRegistry.retrieve(args.injectorKey)?.inject(this) ?: run {
            throw IllegalArgumentException(
                "Failed to retrieve DefaultPaymentAuthenticatorRegistry instance"
            )
        }
    }

    var hasCompleted: Boolean = savedStateHandle.contains(KEY_HAS_COMPLETED)

    suspend fun processChallengeResult(
        challengeResult: ChallengeResult
    ): PaymentFlowResult.Unvalidated {
        return challengeResultProcessor.process(challengeResult)
    }

    suspend fun start3ds2Flow(): NextStep {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds2Fingerprint)
        )

        return runCatching {
            begin3ds2Auth(
                Stripe3ds2Fingerprint(args.nextActionData)
            )
        }.getOrElse {
            NextStep.Complete(
                PaymentFlowResult.Unvalidated(
                    exception = StripeException.create(it)
                )
            )
        }.also {
            savedStateHandle.set(KEY_HAS_COMPLETED, true)
            this.hasCompleted = true
        }
    }

    private suspend fun begin3ds2Auth(
        stripe3ds2Fingerprint: Stripe3ds2Fingerprint
    ): NextStep {
        val transaction = threeDs2Service.createTransaction(
            args.sdkTransactionId,
            stripe3ds2Fingerprint.directoryServerEncryption.directoryServerId,
            messageVersionRegistry.current,
            args.stripeIntent.isLiveMode,
            stripe3ds2Fingerprint.directoryServerName,
            stripe3ds2Fingerprint.directoryServerEncryption.rootCerts,
            stripe3ds2Fingerprint.directoryServerEncryption.directoryServerPublicKey,
            stripe3ds2Fingerprint.directoryServerEncryption.keyId,
            args.config.uiCustomization.uiCustomization
        )

        val timeout = args.config.timeout
        return runCatching {
            perform3ds2AuthenticationRequest(
                transaction,
                stripe3ds2Fingerprint,
                args.requestOptions,
                timeout
            )
        }.fold(
            onSuccess = { authResult ->
                on3ds2AuthSuccess(
                    authResult,
                    transaction,
                    stripe3ds2Fingerprint.source,
                    timeout
                )
            },
            onFailure = {
                NextStep.Complete(
                    PaymentFlowResult.Unvalidated(
                        exception = StripeException.create(it)
                    )
                )
            }
        )
    }

    /**
     * Fire the 3DS2 AReq.
     */
    private suspend fun perform3ds2AuthenticationRequest(
        transaction: Transaction,
        stripe3ds2Fingerprint: Stripe3ds2Fingerprint,
        requestOptions: ApiRequest.Options,
        timeout: Int
    ) = withContext(workContext) {
        val areqParams = transaction.createAuthenticationRequestParameters()

        val authParams = Stripe3ds2AuthParams(
            stripe3ds2Fingerprint.source,
            areqParams.sdkAppId,
            areqParams.sdkReferenceNumber,
            areqParams.sdkTransactionId.value,
            areqParams.deviceData,
            areqParams.sdkEphemeralPublicKey,
            areqParams.messageVersion,
            timeout,
            // We do not currently have a fallback url
            // TODO(smaskell-stripe): Investigate more robust error handling
            returnUrl = null
        )

        requireNotNull(
            stripeRepository.start3ds2Auth(
                authParams,
                requestOptions
            )
        )
    }

    @VisibleForTesting
    internal suspend fun on3ds2AuthSuccess(
        result: Stripe3ds2AuthResult,
        transaction: Transaction,
        sourceId: String,
        timeout: Int
    ): NextStep {
        val ares = result.ares
        return if (ares != null) {
            if (ares.isChallenge) {
                startChallengeFlow(
                    ares,
                    transaction,
                    sourceId,
                    timeout
                )
            } else {
                startFrictionlessFlow()
            }
        } else if (result.fallbackRedirectUrl != null) {
            on3ds2AuthFallback(result.fallbackRedirectUrl)
        } else {
            val errorMessage = result.error?.let { error ->
                listOf(
                    "Code: ${error.errorCode}",
                    "Detail: ${error.errorDetail}",
                    "Description: ${error.errorDescription}",
                    "Component: ${error.errorComponent}"
                ).joinToString(separator = ", ")
            } ?: "Invalid 3DS2 authentication response"

            NextStep.Complete(
                PaymentFlowResult.Unvalidated(
                    exception = StripeException.create(
                        RuntimeException(
                            "Error encountered during 3DS2 authentication request. $errorMessage"
                        )
                    )
                )
            )
        }
    }

    /**
     * Used when standard 3DS2 authentication mechanisms are unavailable.
     */
    private fun on3ds2AuthFallback(
        fallbackRedirectUrl: String
    ): NextStep {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds2Fallback)
        )

        return NextStep.StartFallback(
            PaymentBrowserAuthContract.Args(
                objectId = args.stripeIntent.id.orEmpty(),
                StripePaymentController.getRequestCode(args.stripeIntent),
                args.stripeIntent.clientSecret.orEmpty(),
                fallbackRedirectUrl,
                returnUrl = null,
                enableLogging = args.enableLogging,
                stripeAccountId = args.requestOptions.stripeAccount,
                // 3D-Secure requires cancelling the source when the user cancels auth (AUTHN-47)
                shouldCancelSource = true
            )
        )
    }

    private fun startFrictionlessFlow(): NextStep {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds2Frictionless)
        )

        return NextStep.Complete(
            PaymentFlowResult.Unvalidated(
                clientSecret = args.stripeIntent.clientSecret,
                stripeAccountId = args.requestOptions.stripeAccount
            )
        )
    }

    suspend fun initChallenge(
        args: InitChallengeArgs
    ) = initChallengeRepository.startChallenge(args)

    @VisibleForTesting
    internal suspend fun startChallengeFlow(
        ares: Stripe3ds2AuthResult.Ares,
        transaction: Transaction,
        sourceId: String,
        maxTimeout: Int
    ) = withContext(workContext) {
        delay(StripePaymentController.CHALLENGE_DELAY)

        val challengeParameters = ChallengeParameters(
            acsSignedContent = ares.acsSignedContent,
            threeDsServerTransactionId = ares.threeDSServerTransId,
            acsTransactionId = ares.acsTransId
        )

        NextStep.StartChallenge(
            transaction.createInitChallengeArgs(
                challengeParameters,
                maxTimeout,
                IntentData(
                    args.stripeIntent.clientSecret.orEmpty(),
                    sourceId,
                    args.requestOptions.apiKey,
                    args.requestOptions.stripeAccount
                )
            )
        )
    }

    private companion object {
        private const val KEY_HAS_COMPLETED = "key_next_step"
    }
}

internal sealed class NextStep {
    data class StartChallenge(
        val args: InitChallengeArgs
    ) : NextStep()

    data class StartFallback(
        val args: PaymentBrowserAuthContract.Args
    ) : NextStep()

    data class Complete(
        val result: PaymentFlowResult.Unvalidated
    ) : NextStep()
}

internal class Stripe3ds2TransactionViewModelFactory(
    private val applicationSupplier: () -> Application,
    owner: SavedStateRegistryOwner,
    private val argsSupplier: () -> Stripe3ds2TransactionContract.Args,
    private val workContext: CoroutineContext = Dispatchers.IO
) : AbstractSavedStateViewModelFactory(owner, null) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        val args = argsSupplier()
        return Stripe3ds2TransactionViewModel(
            args,
            InitChallengeRepositoryFactory(
                applicationSupplier(),
                args.stripeIntent.isLiveMode,
                args.sdkTransactionId,
                args.config.uiCustomization.uiCustomization,
                args.fingerprint.directoryServerEncryption.rootCerts,
                args.enableLogging,
                workContext
            ).create(),
            handle
        ) as T
    }
}

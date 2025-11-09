package com.stripe.android.taptoadd

import android.util.Log
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentResult
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.SetupIntentConfiguration
import com.stripe.stripeterminal.external.models.TerminalException
import javax.inject.Provider
import kotlin.coroutines.suspendCoroutine

internal interface TapToAddCollectionHandler {
    suspend fun collect(
        customerId: String,
        allowRedisplay: PaymentMethod.AllowRedisplay,
    ): CollectionState

    sealed interface CollectionState {
        data class Collected(val paymentMethod: PaymentMethod) : CollectionState

        data class FailedCollection(
            val error: Throwable,
            val displayMessage: ResolvableString?,
        ) : CollectionState
    }

    companion object {
        @OptIn(TapToAddPreview::class)
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            connectionManager: TapToAddConnectionManager,
            createCardPresentSetupIntentCallbackProvider: Provider<CreateCardPresentSetupIntentCallback?>,
        ): TapToAddCollectionHandler {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddCollectionHandler(
                    connectionManager = connectionManager,
                    createCardPresentSetupIntentCallbackProvider = createCardPresentSetupIntentCallbackProvider,
                )
            } else {
                UnsupportedTapToAddCollectionHandler()
            }
        }
    }
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddCollectionHandler(
    private val connectionManager: TapToAddConnectionManager,
    private val createCardPresentSetupIntentCallbackProvider: Provider<CreateCardPresentSetupIntentCallback?>,
) : TapToAddCollectionHandler {
    override suspend fun collect(
        customerId: String,
        allowRedisplay: PaymentMethod.AllowRedisplay,
    ): TapToAddCollectionHandler.CollectionState = runCatching {
        if (!connectionManager.isConnected) {
            connectionManager.startConnecting()

            connectionManager
                .awaitConnection()
                .onFailure { exception ->
                    throw exception
                }
        }

        val callback = createCardPresentSetupIntentCallbackProvider.get()
            ?: throw IllegalStateException("No card present setup intent callback!")

        when (val intentResult = callback.createCardPresentSetupIntent(customerId)) {
            is CreateCardPresentSetupIntentResult.Success -> {
                val intent = retrieveSetupIntent(intentResult.clientSecret)
                val collectedIntent = collectPaymentMethod(intent, allowRedisplay.toTerminalAllowRedisplay())
                val confirmedIntent = confirmSetupIntent(collectedIntent)

                return TapToAddCollectionHandler.CollectionState.Collected(
                    paymentMethod = PaymentMethod.Builder()
                        .setCode("card")
                        .setType(PaymentMethod.Type.Card)
                        .setId(confirmedIntent.latestAttempt?.paymentMethodDetails?.cardPresentDetails?.generatedCard)
                        .setCard(
                            PaymentMethod.Card(
                                last4 = confirmedIntent.paymentMethod?.cardPresentDetails?.last4,
                                brand = CardBrand.fromCode(confirmedIntent.paymentMethod?.cardPresentDetails?.brand)
                            )
                        )
                        .build()
                )
            }
            is CreateCardPresentSetupIntentResult.Failure -> {
                return TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = intentResult.cause,
                    displayMessage = intentResult.displayMessage?.resolvableString
                        ?: intentResult.cause.stripeErrorMessage()
                )
            }
        }
    }.fold(
        onSuccess = { it },
        onFailure = { error ->
            TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                displayMessage = error.stripeErrorMessage()
            )
        }
    )

    private suspend fun retrieveSetupIntent(
        clientSecret: String
    ) = suspendCoroutine { continuation ->
        terminal().retrieveSetupIntent(
            clientSecret = clientSecret,
            callback = object : SetupIntentCallback {
                override fun onFailure(e: TerminalException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onSuccess(setupIntent: SetupIntent) {
                    continuation.resumeWith(Result.success(setupIntent))
                }
            }
        )
    }

    private suspend fun collectPaymentMethod(
        intent: SetupIntent,
        allowRedisplay: AllowRedisplay
    ) = suspendCoroutine { continuation ->
        terminal().collectSetupIntentPaymentMethod(
            intent = intent,
            allowRedisplay = allowRedisplay,
            config = SetupIntentConfiguration.Builder().build(),
            callback = object : SetupIntentCallback {
                override fun onFailure(e: TerminalException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onSuccess(setupIntent: SetupIntent) {
                    continuation.resumeWith(Result.success(setupIntent))
                }
            }
        )
    }

    private suspend fun confirmSetupIntent(
        intent: SetupIntent,
    ) = suspendCoroutine { continuation ->
        terminal().confirmSetupIntent(
            intent = intent,
            callback = object : SetupIntentCallback {
                override fun onFailure(e: TerminalException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onSuccess(setupIntent: SetupIntent) {
                    Log.d("TAP-TO-ADD", setupIntent.paymentMethodId ?: "")
                    continuation.resumeWith(Result.success(setupIntent))
                }
            }
        )
    }

    private fun terminal() = Terminal.getInstance()

    private fun PaymentMethod.AllowRedisplay.toTerminalAllowRedisplay() = when (this) {
        PaymentMethod.AllowRedisplay.ALWAYS -> AllowRedisplay.ALWAYS
        PaymentMethod.AllowRedisplay.LIMITED -> AllowRedisplay.LIMITED
        PaymentMethod.AllowRedisplay.UNSPECIFIED -> AllowRedisplay.UNSPECIFIED
    }
}

internal class UnsupportedTapToAddCollectionHandler : TapToAddCollectionHandler {
    override suspend fun collect(
        customerId: String,
        allowRedisplay: PaymentMethod.AllowRedisplay,
    ): TapToAddCollectionHandler.CollectionState {
        return TapToAddCollectionHandler.CollectionState.FailedCollection(
            error = IllegalStateException("Not handled!"),
            displayMessage = null,
        )
    }
}

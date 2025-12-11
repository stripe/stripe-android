package com.stripe.android.common.taptoadd

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.R as PaymentsCoreR

@OptIn(TapToAddPreview::class)
internal interface CreateCardPresentSetupIntentCallbackRetriever {
    suspend fun waitForCallback(): CreateCardPresentSetupIntentCallback
}

@OptIn(TapToAddPreview::class)
internal class DefaultCreateCardPresentSetupIntentCallbackRetriever @Inject constructor(
    private val errorReporter: ErrorReporter,
    private val requestOptionsProvider: Provider<ApiRequest.Options>,
    private val createCardPresentSetupIntentCallbackProvider: Provider<CreateCardPresentSetupIntentCallback?>
) : CreateCardPresentSetupIntentCallbackRetriever {
    override suspend fun waitForCallback(): CreateCardPresentSetupIntentCallback {
        return fetcher() ?: withTimeoutOrNull(PROVIDER_FETCH_TIMEOUT.seconds) {
            while (true) {
                delay(PROVIDER_FETCH_INTERVAL)

                fetcher()?.let {
                    errorReporter.report(
                        errorEvent = ErrorReporter
                            .SuccessEvent
                            .TAP_TO_ADD_FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING
                    )
                    return@withTimeoutOrNull it
                }
            }

            @Suppress("UNREACHABLE_CODE")
            null
        } ?: run {
            errorReporter.report(
                errorEvent = ErrorReporter
                    .ExpectedErrorEvent
                    .CREATE_CARD_PRESENT_SETUP_INTENT_NULL
            )

            val errorMessage = "${CreateCardPresentSetupIntentCallback::class.java.simpleName} must be " +
                "implemented when using Tap to Add!"

            throw CreateCardPresentSetupIntentCallbackNotFoundException(
                message = errorMessage,
                resolvableError = if (requestOptionsProvider.get().apiKeyIsLiveMode) {
                    PaymentsCoreR.string.stripe_internal_error.resolvableString
                } else {
                    errorMessage.resolvableString
                }
            )
        }
    }

    private fun fetcher(): CreateCardPresentSetupIntentCallback? {
        return createCardPresentSetupIntentCallbackProvider.get()
    }

    private companion object {
        const val PROVIDER_FETCH_TIMEOUT = 2
        const val PROVIDER_FETCH_INTERVAL = 5L
    }
}

internal class CreateCardPresentSetupIntentCallbackNotFoundException(
    message: String,
    val resolvableError: ResolvableString
) : StripeException(message = message) {
    override fun analyticsValue(): String = "createCardPresentSetupIntentCallbackNotFound"
}

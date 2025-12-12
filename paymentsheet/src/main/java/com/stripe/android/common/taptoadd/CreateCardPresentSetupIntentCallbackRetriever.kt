package com.stripe.android.common.taptoadd

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackRetriever
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.payments.core.analytics.ErrorReporter.SuccessEvent
import javax.inject.Inject
import javax.inject.Provider

@OptIn(TapToAddPreview::class)
internal interface CreateCardPresentSetupIntentCallbackRetriever {
    suspend fun waitForCallback(): CreateCardPresentSetupIntentCallback
}

@OptIn(TapToAddPreview::class)
internal class DefaultCreateCardPresentSetupIntentCallbackRetriever @Inject constructor(
    errorReporter: ErrorReporter,
    requestOptionsProvider: Provider<ApiRequest.Options>,
    private val createCardPresentSetupIntentCallbackProvider: Provider<CreateCardPresentSetupIntentCallback?>
) : CallbackRetriever(errorReporter, requestOptionsProvider), CreateCardPresentSetupIntentCallbackRetriever {
    override suspend fun waitForCallback(): CreateCardPresentSetupIntentCallback {
        return waitForCallback(
            neededWaitEvent = SuccessEvent.FOUND_CREATE_CARD_PRESENT_SETUP_INTENT_CALLBACK_WHILE_POLLING,
            notFoundEvent = ExpectedErrorEvent.CREATE_CARD_PRESENT_SETUP_INTENT_CALLBACK_NULL,
            analyticsValue = ANALYTICS_VALUE,
            notFoundMessage = "${CreateCardPresentSetupIntentCallback::class.java.simpleName} must be " +
                "implemented when using Tap to Add!",
        ) {
            createCardPresentSetupIntentCallbackProvider.get()
        }
    }

    private companion object {
        const val ANALYTICS_VALUE = "cardPresentSetupIntentCallbackNotFound"
    }
}

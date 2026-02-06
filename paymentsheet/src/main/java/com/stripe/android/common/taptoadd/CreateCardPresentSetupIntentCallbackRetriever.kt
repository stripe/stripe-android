package com.stripe.android.common.taptoadd

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackRetriever
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.payments.core.analytics.ErrorReporter.SuccessEvent
import com.stripe.android.paymentsheet.CreateIntentResult
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

        return object : CreateCardPresentSetupIntentCallback {
            override suspend fun createCardPresentSetupIntent(): CreateIntentResult {
                return CreateIntentResult.Success(
                    "seti_1SxgCb2UPI6VfMquUpKcjpAj_secret_TvXHtqwrlXS6JLfKILdhyqr6AhvWFcg"
                )
            }

        }
    }

    private companion object {
        const val ANALYTICS_VALUE = "cardPresentSetupIntentCallbackNotFound"
    }
}

package com.stripe.android.paymentelement.embedded

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import com.stripe.android.paymentelement.embedded.content.EmbeddedStateHelper
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuth
import javax.inject.Inject

internal interface EmbeddedResultCallbackHelper {
    fun setResult(result: EmbeddedPaymentElement.Result)
}

@EmbeddedPaymentElementScope
internal class DefaultEmbeddedResultCallbackHelper @Inject constructor(
    private val resultCallback: EmbeddedPaymentElement.ResultCallback,
    private val stateHelper: EmbeddedStateHelper,
    private val linkInlineAuth: EmbeddedLinkInlineAuth,
) : EmbeddedResultCallbackHelper {

    override fun setResult(result: EmbeddedPaymentElement.Result) {
        resultCallback.onResult(result)
        if (result is EmbeddedPaymentElement.Result.Completed) {
            linkInlineAuth.onIntentConfirmed()
            stateHelper.state = null
        }
    }
}

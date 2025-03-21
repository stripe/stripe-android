@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import com.stripe.android.paymentelement.embedded.content.EmbeddedStateHelper
import javax.inject.Inject

internal interface EmbeddedResultCallbackHelper {
    fun setResult(result: EmbeddedPaymentElement.Result)
}

@EmbeddedPaymentElementScope
internal class DefaultEmbeddedResultCallbackHelper @Inject constructor(
    private val resultCallback: EmbeddedPaymentElement.ResultCallback,
    private val stateHelper: EmbeddedStateHelper
) : EmbeddedResultCallbackHelper {

    override fun setResult(result: EmbeddedPaymentElement.Result) {
        resultCallback.onResult(result)
        if (result is EmbeddedPaymentElement.Result.Completed) {
            stateHelper.state = null
        }
    }
}

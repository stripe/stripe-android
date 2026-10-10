package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedPaymentElementStateHolder @Inject constructor() {
    var lifecycleOwner: LifecycleOwner? = null
    var element: EmbeddedPaymentElement? = null
    var resultCallback: EmbeddedPaymentElement.ResultCallback? = null

    fun clear(owner: LifecycleOwner) {
        if (lifecycleOwner === owner) clear()
    }

    fun clear() {
        lifecycleOwner = null
        element = null
        resultCallback = null
    }
}

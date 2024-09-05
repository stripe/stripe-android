package com.stripe.android.paymentsheet

@OptIn(ExperimentalCvcRecollectionApi::class)
internal object CvcRecollectionCallbackHandler {
    var isCvcRecollectionEnabledCallback: CvcRecollectionEnabledCallback? = null

    fun isCvcRecollectionEnabledForDeferredIntent(): Boolean {
        return isCvcRecollectionEnabledCallback?.isCvcRecollectionRequired() == true
    }
}

package com.stripe.android.paymentsheet

import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal object AnalyticEventInterceptor {
    var analyticEventCallback: AnalyticEventCallback? = null
}

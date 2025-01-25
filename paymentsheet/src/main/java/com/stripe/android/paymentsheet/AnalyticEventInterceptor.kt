package com.stripe.android.paymentsheet

import com.stripe.android.paymentelement.AnalyticEventCallback

internal object AnalyticEventInterceptor {
    var analyticEventCallback: AnalyticEventCallback? = null
}

package com.stripe.android.polling

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PollingAnalyticsEventReporter {
    fun onPollingTimedOut(paymentMethodType: String, lastKnownStatus: String?)
}

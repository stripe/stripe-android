package com.stripe.android.paymentsheet.analytics

import javax.inject.Inject

internal class EventTimeProvider @Inject constructor() {
    fun currentTimeMillis() = System.currentTimeMillis()
}

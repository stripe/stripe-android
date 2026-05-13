package com.stripe.android.paymentsheet.state

import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.Deferred

internal typealias PrefetchedPaymentMethods = Deferred<Result<List<PaymentMethod>>>

package com.stripe.android.paymentsheet.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

internal fun CoroutineScope.childScope(context: CoroutineContext): CoroutineScope {
    return CoroutineScope(context + SupervisorJob(coroutineContext[Job]))
}

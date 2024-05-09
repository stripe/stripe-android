package com.stripe.android.uicore.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine as flowCombine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T1, T2, T3> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
): Flow<Triple<T1, T2, T3>> {
    return flowCombine(flow1, flow2, flow3) { flow1Value, flow2Value, flow3Value ->
        Triple(flow1Value, flow2Value, flow3Value)
    }
}

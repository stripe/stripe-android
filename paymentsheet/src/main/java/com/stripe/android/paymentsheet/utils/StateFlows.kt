package com.stripe.android.paymentsheet.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal fun <T1, T2, R> ViewModel.combineStateFlows(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    transform: (T1, T2) -> R,
): StateFlow<R> {
    return combine(flow1, flow2, transform).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = transform(flow1.value, flow2.value),
    )
}

internal fun <T1, T2, T3, T4, T5, T6, T7, R> ViewModel.combineStateFlows(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    flow3: StateFlow<T3>,
    flow4: StateFlow<T4>,
    flow5: StateFlow<T5>,
    flow6: StateFlow<T6>,
    flow7: StateFlow<T7>,
    transform: (T1, T2, T3, T4, T5, T6, T7) -> R,
): StateFlow<R> {
    val initialValue = transform(
        flow1.value,
        flow2.value,
        flow3.value,
        flow4.value,
        flow5.value,
        flow6.value,
        flow7.value,
    )

    return combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { items ->
        @Suppress("UNCHECKED_CAST")
        transform(
            items[0] as T1,
            items[1] as T2,
            items[2] as T3,
            items[3] as T4,
            items[4] as T5,
            items[5] as T6,
            items[6] as T7,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = initialValue,
    )
}

context (ViewModel)
internal fun <T, R> StateFlow<T>.mapAsStateFlow(
    started: SharingStarted = SharingStarted.WhileSubscribed(),
    transform: (T) -> R,
): StateFlow<R> {
    return map {
        transform(it)
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = transform(value),
    )
}

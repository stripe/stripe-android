package com.stripe.android.uicore.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A subclass of [StateFlow] that allows us to turn a [Flow] into a [StateFlow].
 *
 * @param flow The [Flow] that should be converted to a [StateFlow]
 * @param produceValue The producer of the [StateFlow's] value
 */
private class FlowToStateFlow<T>(
    private val flow: Flow<T>,
    private val produceValue: () -> T,
) : StateFlow<T> {

    override val replayCache: List<T>
        get() = listOf(value)

    override val value: T
        get() = produceValue()

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        val collectorJob = currentCoroutineContext()[Job]
        flow.collect(collector)

        try {
            while (true) {
                collectorJob?.ensureActive()
            }
        } finally {
            // Nothing to do here
        }
    }
}

/**
 * Maps one [StateFlow] into another, instead of loosening it to a [Flow].
 *
 * @param transform The transformation from one type to another
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T, R> StateFlow<T>.mapAsStateFlow(
    transform: (T) -> R,
): StateFlow<R> {
    return FlowToStateFlow(
        flow = map(transform),
        produceValue = { transform(value) },
    )
}

/**
 * Combines two [StateFlow]s into another, instead of loosening the result to a [Flow].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T1, T2, R> combineAsStateFlow(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    transform: (T1, T2) -> R,
): StateFlow<R> {
    return FlowToStateFlow(
        flow = combine(flow1, flow2, transform).distinctUntilChanged(),
        produceValue = { transform(flow1.value, flow2.value) },
    )
}

/**
 * Combines three [StateFlow]s into another, instead of loosening the result to a [Flow].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T1, T2, T3, R> combineAsStateFlow(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    flow3: StateFlow<T3>,
    transform: (T1, T2, T3) -> R,
): StateFlow<R> {
    return FlowToStateFlow(
        flow = combine(flow1, flow2, flow3, transform).distinctUntilChanged(),
        produceValue = { transform(flow1.value, flow2.value, flow3.value) },
    )
}

/**
 * Combines three [StateFlow]s into another, instead of loosening the result to a [Flow].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T1, T2, T3, T4, T5, T6, R> combineAsStateFlow(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    flow3: StateFlow<T3>,
    flow4: StateFlow<T4>,
    flow5: StateFlow<T5>,
    flow6: StateFlow<T6>,
    transform: (T1, T2, T3, T4, T5, T6) -> R,
): StateFlow<R> {
    @Suppress("UNCHECKED_CAST")
    return FlowToStateFlow(
        flow = combine(listOf(flow1, flow2, flow3, flow4, flow5, flow6)) { values ->
            val flow1Value = values[0] as T1
            val flow2Value = values[1] as T2
            val flow3Value = values[2] as T3
            val flow4Value = values[3] as T4
            val flow5Value = values[4] as T5
            val flow6Value = values[5] as T6
            transform(flow1Value, flow2Value, flow3Value, flow4Value, flow5Value, flow6Value)
        },
        produceValue = { transform(flow1.value, flow2.value, flow3.value, flow4.value, flow5.value, flow6.value) },
    )
}

/**
 * Creates a [StateFlow] that only ever emits the provided value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> stateFlowOf(value: T) = MutableStateFlow(value).asStateFlow()

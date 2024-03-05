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
        flow = combine(flow1, flow2, transform),
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
        flow = combine(flow1, flow2, flow3, transform),
        produceValue = { transform(flow1.value, flow2.value, flow3.value) },
    )
}

/**
 * Creates a [StateFlow] that only ever emits the provided value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> stateFlowOf(value: T) = MutableStateFlow(value).asStateFlow()

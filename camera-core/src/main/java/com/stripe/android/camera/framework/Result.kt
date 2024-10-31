package com.stripe.android.camera.framework

import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.camera.framework.util.FrameRateTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A result handler for data processing. This is called when results are available from an
 * [Analyzer].
 */
internal interface ResultHandler<Input, Output, Verdict> {
    suspend fun onResult(result: Output, data: Input): Verdict
}

/**
 * A specialized result handler that has some form of state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class StatefulResultHandler<Input, State, Output, Verdict>(
    private var initialState: State
) : ResultHandler<Input, Output, Verdict> {

    /**
     * The state of the result handler. This can be read, but not updated by analyzers.
     */
    var state: State = initialState
        protected set

    /**
     * Reset the state to the initial value.
     */
    protected open fun reset() { state = initialState }
}

/**
 * A result handler with a method that notifies when all data has been processed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class TerminatingResultHandler<Input, State, Output>(
    initialState: State
) : StatefulResultHandler<Input, State, Output, Unit>(initialState) {
    /**
     * All data has been processed and termination was reached.
     */
    abstract suspend fun onAllDataProcessed()

    /**
     * Not all data was processed before termination.
     */
    abstract suspend fun onTerminatedEarly()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AggregateResultListener<InterimResult, FinalResult> {

    /**
     * The aggregated result of an [AnalyzerLoop] is available.
     *
     * @param result: the result from the Aggregator
     */
    suspend fun onResult(result: FinalResult)

    /**
     * An interim result is available, but the [AnalyzerLoop] is still processing more data frames.
     * This is useful for displaying a debug window or handling state updates during a scan.
     *
     * @param result: the result from the [AnalyzerLoop]
     */
    suspend fun onInterimResult(result: InterimResult)

    /**
     * The result aggregator was reset back to its original state.
     */
    suspend fun onReset()
}

/**
 * The [ResultAggregator] processes results from analyzers until a condition is met. That condition
 * is part of the aggregator's logic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class ResultAggregator<
    DataFrame,
    State,
    AnalyzerResult,
    InterimResult,
    FinalResult
    >(
    private val listener: AggregateResultListener<InterimResult, FinalResult>,
    private val initialState: State
) : StatefulResultHandler<DataFrame, State, AnalyzerResult, Boolean>(initialState),
    LifecycleEventObserver {

    private val finalResultMutex = Mutex()

    private var isCanceled = false
    private var isPaused = false
    private var isFinished = false

    private val frameRateTracker by lazy { FrameRateTracker(this::class.java.simpleName) }

    /**
     * Reset the state of the aggregator and pause aggregation. This is useful for aggregators that
     * can be backgrounded. For example, a user that is scanning an object, but then backgrounds the
     * scanning app. In the case that the scan should be restarted, this feature pauses the result
     * handlers and resets the state.
     */
    private fun resetAndPause() {
        reset()
        isPaused = true
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> resetAndPause()
            Lifecycle.Event.ON_RESUME -> resume()
            Lifecycle.Event.ON_CREATE -> Unit // not used
            Lifecycle.Event.ON_START -> Unit // not used
            Lifecycle.Event.ON_STOP -> Unit // not used
            Lifecycle.Event.ON_DESTROY -> Unit // not used
            Lifecycle.Event.ON_ANY -> Unit // not used
        }
    }

    /**
     * Resume aggregation after it has been paused.
     */
    private fun resume() {
        isPaused = false
    }

    /**
     * Cancel a result aggregator. This means that the result aggregator will ignore all further
     * results and will never return a final result.
     */
    fun cancel() {
        reset()
        isCanceled = true
    }

    /**
     * Bind this result aggregator to a lifecycle. This allows the result aggregator to pause and
     * reset when the lifecycle owner pauses.
     */
    open fun bindToLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * Reset the state of the aggregator. This is useful for aggregating data that can become
     * invalid, such as when a user is scanning an object, and moves the object away from the camera
     * before the scan has completed.
     */
    override fun reset() {
        super.reset()
        isPaused = false
        isCanceled = false
        isFinished = false

        state = initialState

        frameRateTracker.reset()
        runBlocking { listener.onReset() }
    }

    override suspend fun onResult(result: AnalyzerResult, data: DataFrame): Boolean = when {
        isPaused -> false
        isCanceled || isFinished -> true
        else -> withContext(Dispatchers.Default) {
            frameRateTracker.trackFrameProcessed()

            finalResultMutex.withLock {
                val (interimResult, finalResult) = aggregateResult(data, result)

                listener.onInterimResult(interimResult)

                if (!isFinished && finalResult != null) {
                    isFinished = true
                    listener.onResult(finalResult)
                }
                isFinished
            }
        }
    }

    /**
     * Aggregate a new result. If this method returns a non-null [FinalResult], the aggregator will
     * stop listening for new results.
     *
     * @param result: The result to aggregate
     */
    abstract suspend fun aggregateResult(
        frame: DataFrame,
        result: AnalyzerResult
    ): Pair<InterimResult, FinalResult?>
}

package com.stripe.android.camera.framework

import androidx.annotation.RestrictTo
import java.io.Closeable

/**
 * The default number of analyzers to run in parallel.
 */
internal const val DEFAULT_ANALYZER_PARALLEL_COUNT = 2

/**
 * An analyzer takes some data as an input, and returns an analyzed output. Analyzers should not
 * contain any state. They must define whether they can run on a multithreaded executor, and provide
 * a means of analyzing input data to return some form of result.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Analyzer<Input, State, Output> {
    suspend fun analyze(data: Input, state: State): Output
}

/**
 * A factory to create analyzers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AnalyzerFactory<Input, State, Output, AnalyzerType :
        Analyzer<Input, State, Output>> {
    suspend fun newInstance(): AnalyzerType?
}

/**
 * A pool of analyzers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyzerPool<DataFrame, State, Output>(
    val desiredAnalyzerCount: Int,
    val analyzers: List<Analyzer<DataFrame, State, Output>>
) {
    companion object {
        suspend fun <DataFrame, State, Output> of(
            analyzerFactory: AnalyzerFactory<DataFrame, State, Output,
                out Analyzer<DataFrame, State, Output>>,
            desiredAnalyzerCount: Int = DEFAULT_ANALYZER_PARALLEL_COUNT
        ) = AnalyzerPool(
            desiredAnalyzerCount = desiredAnalyzerCount,
            analyzers = (0 until desiredAnalyzerCount).mapNotNull { analyzerFactory.newInstance() }
        )
    }

    fun closeAllAnalyzers() {
        // This should be using analyzers.forEach, but doing so seems to require API 24. It's
        // unclear why this won't use the kotlin.collections version of `forEach`, but it's not
        // during compile.
        for (it in analyzers) { if (it is Closeable) it.close() }
    }
}

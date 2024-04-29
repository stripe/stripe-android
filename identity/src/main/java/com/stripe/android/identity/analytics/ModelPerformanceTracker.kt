package com.stripe.android.identity.analytics

import com.stripe.android.camera.framework.StatTracker
import com.stripe.android.camera.framework.StatTrackerImpl
import com.stripe.android.camera.framework.TaskStats
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Tracker for model performance.
 */
internal class ModelPerformanceTracker @Inject constructor(
    private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
) {

    private val preprocessStats = mutableListOf<TaskStats>()
    private val inferenceStats = mutableListOf<TaskStats>()

    private fun List<TaskStats>.averageDuration() =
        (
            this.fold(Duration.ZERO) { accDuration, next ->
                accDuration + next.duration
            } / size
            ).inWholeMilliseconds

    fun trackPreprocess(): StatTracker =
        StatTrackerImpl { startedAt, _ ->
            preprocessStats += TaskStats(
                startedAt,
                startedAt.elapsedNow(),
                null
            )
        }

    fun trackInference(): StatTracker =
        StatTrackerImpl { startedAt, _ ->
            inferenceStats += TaskStats(
                startedAt,
                startedAt.elapsedNow(),
                null
            )
        }

    suspend fun reportAndReset(mlModel: String) {
        identityAnalyticsRequestFactory.modelPerformance(
            mlModel = mlModel,
            preprocess = preprocessStats.averageDuration(),
            inference = inferenceStats.averageDuration(),
            frames = preprocessStats.size
        )
        preprocessStats.clear()
        inferenceStats.clear()
    }
}

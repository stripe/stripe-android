package com.stripe.android.identity.analytics

import com.stripe.android.camera.framework.StatTracker
import com.stripe.android.camera.framework.StatTrackerImpl
import com.stripe.android.camera.framework.TaskStats
import com.stripe.android.camera.framework.time.Duration
import com.stripe.android.identity.networking.IdentityRepository
import javax.inject.Inject

/**
 * Tracker for model performance.
 */
internal class ModelPerformanceTracker @Inject constructor(
    private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    private val identityRepository: IdentityRepository
) {

    private val preprocessStats = mutableListOf<TaskStats>()
    private val inferenceStats = mutableListOf<TaskStats>()

    private fun List<TaskStats>.averageDuration() =
        (
            this.fold(Duration.ZERO) { accDuration, next ->
                accDuration + next.duration
            } / size
            ).inMilliseconds.toLong()

    fun trackPreprocess(): StatTracker =
        StatTrackerImpl { startedAt, _ ->
            preprocessStats += TaskStats(
                startedAt,
                startedAt.elapsedSince(),
                null
            )
        }

    fun trackInference(): StatTracker =
        StatTrackerImpl { startedAt, _ ->
            inferenceStats += TaskStats(
                startedAt,
                startedAt.elapsedSince(),
                null
            )
        }

    suspend fun reportAndReset(mlModel: String) {
        identityRepository.sendAnalyticsRequest(
            identityAnalyticsRequestFactory.modelPerformance(
                mlModel = mlModel,
                preprocess = preprocessStats.averageDuration(),
                inference = inferenceStats.averageDuration(),
                frames = preprocessStats.size
            )
        )
        preprocessStats.clear()
        inferenceStats.clear()
    }
}

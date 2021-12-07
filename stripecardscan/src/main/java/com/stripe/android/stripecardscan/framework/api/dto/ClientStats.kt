package com.stripe.android.stripecardscan.framework.api.dto

import androidx.annotation.RestrictTo
import com.stripe.android.stripecardscan.framework.RepeatingTaskStats
import com.stripe.android.stripecardscan.framework.Stats
import com.stripe.android.stripecardscan.framework.TaskStats
import com.stripe.android.stripecardscan.framework.ml.ModelLoadDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StatsPayload(
    @SerialName("instance_id") val instanceId: String,
    @SerialName("scan_id") val scanId: String?,
    @SerialName("payload_version") val payloadVersion: Int = 2,
    @SerialName("device") val device: ClientDevice,
    @SerialName("app") val app: AppInfo,
    @SerialName("scan_stats") val scanStats: ScanStatistics,
// TODO: these should probably be reported as part of scanstats
//    @SerialName("model_versions") val modelVersions: List<ModelVersion>,
)

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ScanStatistics(
    @SerialName("tasks") val tasks: Map<String, List<TaskStatistics>>,
    @SerialName("repeating_tasks") val repeatingTasks: Map<String, RepeatingTaskStatistics>,
) {
    companion object {
        @JvmStatic
        fun fromStats() = ScanStatistics(
            tasks = Stats.getTasks().mapValues { entry ->
                entry.value.map { TaskStatistics.fromTaskStats(it) }
            },
            repeatingTasks = Stats.getRepeatingTasks().mapValues { repeatingTask ->
                RepeatingTaskStatistics(
                    executions = repeatingTask.value.map { resultMap -> resultMap.value.executions }
                        .sum()
                )
            }
        )
    }
}

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ModelVersion(
    @SerialName("name") val name: String,
    @SerialName("version") val version: String,
    @SerialName("framework_version") val frameworkVersion: Int,
    @SerialName("loaded_successfully") val loadedSuccessfully: Boolean
) {
    companion object {
        internal fun fromModelLoadDetails(details: ModelLoadDetails) = ModelVersion(
            name = details.modelClass,
            version = details.modelVersion,
            frameworkVersion = details.modelFrameworkVersion,
            loadedSuccessfully = details.success
        )
    }
}

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class TaskStatistics(
    @SerialName("started_at_ms") val startedAtMs: Long,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("result") val result: String?
) {
    companion object {
        @JvmStatic
        internal fun fromTaskStats(taskStats: TaskStats) = TaskStatistics(
            startedAtMs = taskStats.started.toMillisecondsSinceEpoch(),
            durationMs = taskStats.duration.inMilliseconds.toLong(),
            result = taskStats.result
        )
    }
}

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class RepeatingTaskStatistics(
    @SerialName("executions") val executions: Int,
// TODO: these should probably be reported as part of scanstats
//    @SerialName("result") val result: String,
//    @SerialName("start_time_ms") val startTimeMs: Long,
//    @SerialName("total_duration_ms") val totalDurationMs: Long,
//    @SerialName("total_cpu_duration_ms") val totalCpuDurationMs: Long,
//    @SerialName("average_duration_ms") val averageDurationMs: Long,
//    @SerialName("minimum_duration_ms") val minimumDurationMs: Long,
//    @SerialName("maximum_duration_ms") val maximumDurationMs: Long,
) {
// TODO: these should probably be reported as part of scanstats
//    companion object {
//        @JvmStatic
//        internal fun fromRepeatingTaskStats(
//            result: String,
//            repeatingTaskStats: RepeatingTaskStats,
//        ) = RepeatingTaskStatistics(
//            executions = repeatingTaskStats.executions,
//            result = result,
//            startTimeMs = repeatingTaskStats.startedAt.toMillisecondsSinceEpoch(),
//            totalDurationMs = repeatingTaskStats.totalDuration.inMilliseconds.toLong(),
//            totalCpuDurationMs = repeatingTaskStats.totalCpuDuration.inMilliseconds.toLong(),
//            averageDurationMs = repeatingTaskStats.averageDuration().inMilliseconds.toLong(),
//            minimumDurationMs = repeatingTaskStats.minimumDuration.inMilliseconds.toLong(),
//            maximumDurationMs = repeatingTaskStats.maximumDuration.inMilliseconds.toLong(),
//        )
//    }
}

package com.stripe.android.stripecardscan.framework.api.dto

import androidx.annotation.RestrictTo
import com.stripe.android.camera.framework.Stats
import com.stripe.android.camera.framework.TaskStats
import com.stripe.android.stripecardscan.framework.ml.ModelLoadDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class StatsPayload(
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
internal data class ScanStatistics(
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
internal data class ModelVersion(
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
internal data class TaskStatistics(
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
internal data class RepeatingTaskStatistics(
    @SerialName("executions") val executions: Int,
)

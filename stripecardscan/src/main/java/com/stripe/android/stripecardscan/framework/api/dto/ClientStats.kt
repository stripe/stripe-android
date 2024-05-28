package com.stripe.android.stripecardscan.framework.api.dto

import com.stripe.android.camera.framework.Stats
import com.stripe.android.camera.framework.TaskStats
import com.stripe.android.stripecardscan.framework.ml.ModelLoadDetails
import com.stripe.android.stripecardscan.framework.util.ScanConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StatsPayload(
    @SerialName("instance_id") val instanceId: String,
    @SerialName("scan_id") val scanId: String?,
    @SerialName("payload_version") val payloadVersion: Int = 2,
    @SerialName("device") val device: ClientDevice,
    @SerialName("app") val app: AppInfo,
    @SerialName("scan_stats") val scanStats: ScanStatistics,
    @SerialName("configuration") val configuration: ConfigurationStats,
    @SerialName("payload_info") val payloadInfo: PayloadInfo? = null
// TODO: these should probably be reported as part of scanstats
//    @SerialName("model_versions") val modelVersions: List<ModelVersion>,
)

@Serializable
internal data class ScanStatistics(
    @SerialName("tasks") val tasks: Map<String, List<TaskStatistics>>,
    @SerialName("repeating_tasks") val repeatingTasks: Map<String, RepeatingTaskStatistics>
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
internal data class RepeatingTaskStatistics(
    @SerialName("executions") val executions: Int
)

@Serializable
internal data class ConfigurationStats(
    @SerialName("strict_mode_frames") val strictModeFrames: Int
) {
    companion object {
        @JvmStatic
        internal fun fromScanConfig(scanConfig: ScanConfig) = ConfigurationStats(
            strictModeFrames = scanConfig.strictModeFrameCount
        )
    }
}

@Serializable
internal data class PayloadInfo(
    @SerialName("image_compression_type") val imageCompressionType: String,
    @SerialName("image_compression_quality") val imageCompressionQuality: Float,
    @SerialName("image_payload_size") val imagePayloadSizeInBytes: Int,
    @SerialName("image_payload_count") val imagePayloadCount: Int,
    @SerialName("image_payload_max_count") val imagePayloadMaxCount: Int
)

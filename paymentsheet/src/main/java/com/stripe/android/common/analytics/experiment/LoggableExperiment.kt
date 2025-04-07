package com.stripe.android.common.analytics.experiment

import com.stripe.android.utils.filterNotNullValues

/**
 * A base class for all experiments that are logged to Ursula.
 */
internal sealed class LoggableExperiment(
    open val name: String,
    open val arbId: String,
    open val group: String,
    open val dimensions: Map<String, String>
) {
    data class LinkGlobalHoldback(
        override val arbId: String,
        override val group: String,
        val isReturningLinkConsumer: Boolean,
        val useLinkNative: Boolean,
        val emailRecognitionSource: EmailRecognitionSource?,
        val providedDefaultValues: ProvidedDefaultValues,
        val spmEnabled: Boolean,
        val integrationShape: String
    ) : LoggableExperiment(
        arbId = arbId,
        group = group,
        name = "link_global_holdback",
        dimensions = mapOf(
            "integration_type" to "mpe_android",
            "is_returning_link_consumer" to isReturningLinkConsumer.toString(),
            "dvs_provided" to providedDefaultValues.toDimension(),
            "use_link_native" to useLinkNative.toString(),
            "email_recognition_source" to emailRecognitionSource?.dimension,
            "spm_enabled" to spmEnabled.toString(),
            "integration_shape" to integrationShape,
        ).filterNotNullValues()
    ) {
        enum class EmailRecognitionSource(val dimension: String) {
            EMAIL("email"),
        }

        data class ProvidedDefaultValues(
            val email: Boolean,
            val name: Boolean,
            val phone: Boolean,
        ) {
            fun toDimension(): String = listOfNotNull(
                if (email) "email" else null,
                if (name) "name" else null,
                if (phone) "phone" else null
            ).joinToString(",")
        }
    }
}

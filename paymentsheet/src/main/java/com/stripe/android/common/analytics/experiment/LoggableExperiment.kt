package com.stripe.android.common.analytics.experiment

import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.utils.filterNotNullValues

/**
 * A base class for all experiments that are logged to Ursula.
 */
internal sealed class LoggableExperiment(
    open val experiment: ExperimentAssignment,
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
        val integrationShape: String,
        val linkDisplayed: Boolean
    ) : LoggableExperiment(
        arbId = arbId,
        group = group,
        experiment = ExperimentAssignment.LINK_GLOBAL_HOLD_BACK,
        dimensions = mapOf(
            "integration_type" to "mpe_android",
            "is_returning_link_consumer" to isReturningLinkConsumer.toString(),
            "dvs_provided" to providedDefaultValues.toDimension(),
            "use_link_native" to useLinkNative.toString(),
            "recognition_type" to emailRecognitionSource?.dimension,
            "has_spms" to spmEnabled.toString(),
            "integration_shape" to integrationShape,
            "link_displayed" to linkDisplayed.toString(),
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

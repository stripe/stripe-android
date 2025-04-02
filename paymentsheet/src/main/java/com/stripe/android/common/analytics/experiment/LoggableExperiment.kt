package com.stripe.android.common.analytics.experiment

/**
 * A base class for all experiments that are logged to Ursula.
 */
internal sealed class LoggableExperiment(
    open val name: String,
    open val arbId: String,
    open val group: ExperimentGroup,
    open val dimensions: Map<String, String>
) {
    data class LinkGlobalHoldback(
        override val arbId: String,
        override val group: ExperimentGroup,
        private val isReturningLinkUser: Boolean,
        private val isLinkNative: Boolean
    ) : LoggableExperiment(
        arbId = arbId,
        group = group,
        name = "link_global_holdback",
        dimensions = mapOf(
            "integration_type" to "mpe",
            "link_default_opt_in" to "true", // TBD
            "is_returning_link_user" to isReturningLinkUser.toString(),
            "dvs_provided" to "", // TBD
            "recognition_type" to "email", // TBD
            "link_native" to isLinkNative.toString()
        )
    )
}

internal enum class ExperimentGroup(val groupName: String) {
    CONTROL("control"),
    TREATMENT("treatment")
}

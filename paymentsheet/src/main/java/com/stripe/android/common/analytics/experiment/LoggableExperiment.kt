package com.stripe.android.common.analytics.experiment

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
    ) : LoggableExperiment(
        arbId = arbId,
        group = group,
        name = "link_global_holdback",
        dimensions = mapOf(
            "integration_type" to "mpe",
            "is_returning_link_consumer" to isReturningLinkConsumer.toString(),
        )
    )
}

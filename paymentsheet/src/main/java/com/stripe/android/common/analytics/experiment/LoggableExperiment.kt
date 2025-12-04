package com.stripe.android.common.analytics.experiment

import com.stripe.android.model.ElementsSession
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

    data class OcsMobileHorizontalAA(
        override val experiment: ExperimentAssignment,
        override val group: String,
        val experimentsData: ElementsSession.ExperimentsData,
        val commonElementsDimensions: CommonElementsDimensions,
        val numSavedPaymentMethods: Int?,
    ) : LoggableExperiment(
        experiment = experiment,
        arbId = experimentsData.arbId,
        group = group,
        // TODO: it looks like even if something is included in our analytics elsewhere, we need to append with "dimension-" for it to work as a dimension?
        dimensions = commonElementsDimensions.toDimensions() + mapOf(
            "num_saved_payment_methods" to numSavedPaymentMethods?.toString(),
        ).filterNotNullValues(),
    )

    data class LinkHoldback constructor(
        override val arbId: String,
        override val group: String,
        override val experiment: ExperimentAssignment,
        val isReturningLinkUser: Boolean,
        val useLinkNative: Boolean,
        val emailRecognitionSource: EmailRecognitionSource?,
        val providedDefaultValues: ProvidedDefaultValues,
        val spmEnabled: Boolean,
        val integrationShape: String,
        val linkDisplayed: Boolean,
        val elementsSessionId: String,
        val mobileSdkVersion: String,
        val mobileSessionId: String
    ) : LoggableExperiment(
        arbId = arbId,
        group = group,
        experiment = experiment,
        dimensions = mapOf(
            "integration_type" to "mpe_android",
            "is_returning_link_user" to isReturningLinkUser.toString(),
            "dvs_provided" to providedDefaultValues.toDimension(),
            "use_link_native" to useLinkNative.toString(),
            "recognition_type" to emailRecognitionSource?.dimension,
            "has_spms" to spmEnabled.toString(),
            "integration_shape" to integrationShape,
            "link_displayed" to linkDisplayed.toString(),
            "mobile_sdk_version" to mobileSdkVersion,
            "elements_session_id" to elementsSessionId,
            "mobile_session_id" to mobileSessionId
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
            ).joinToString(" ")
        }
    }
}

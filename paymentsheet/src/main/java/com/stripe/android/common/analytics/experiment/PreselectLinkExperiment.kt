package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

internal interface PreselectLinkExperiment {
    // Call only when the existing selection priorities have been exhausted.
    fun getSelection(
        metadata: PaymentMethodMetadata,
        customer: CustomerState?,
        integrationConfiguration: PaymentElementLoader.Configuration,
    ): PaymentSelection.Link?
}

internal class DefaultPreselectLinkExperiment @Inject constructor(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
) : PreselectLinkExperiment {
    override fun getSelection(
        metadata: PaymentMethodMetadata,
        customer: CustomerState?,
        integrationConfiguration: PaymentElementLoader.Configuration,
    ): PaymentSelection.Link? {
        if (mode == EventReporter.Mode.Complete) return null

        when (integrationConfiguration) {
            is PaymentElementLoader.Configuration.PaymentSheet,
            is PaymentElementLoader.Configuration.Embedded -> Unit
            else -> return null
        }

        val configuration = integrationConfiguration.commonConfiguration
        // Exclude these cases because Link would be hidden in the Payment Element.
        if (configuration.link.display != PaymentSheet.LinkConfiguration.Display.Automatic) return null
        if (configuration.walletButtons?.willDisplayExternally == true) return null
        val brand = metadata.linkState?.configuration?.linkBrand ?: return null

        val experimentsData = metadata.experimentsData ?: return null
        val variant = experimentsData.experimentAssignments[ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK]
        val isEnabled = when (variant) {
            "control", "control_test" -> false
            "treatment" -> true
            // Preserve existing behavior and do not log unimplemented variants.
            else -> return null
        }

        eventReporter.onExperimentExposure(
            experiment = LoggableExperiment.OcsMobilePreselectLink(
                experimentsData = experimentsData,
                group = variant,
                metadata = metadata,
                mode = mode,
                hasSavedPaymentMethod = !customer?.paymentMethods.isNullOrEmpty(),
            ),
            paymentMethodMetadata = metadata,
        )
        return if (isEnabled) PaymentSelection.Link(brand = brand) else null
    }
}

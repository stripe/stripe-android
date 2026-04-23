package com.stripe.android.common.analytics.experiment

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

internal interface LogCardArtExperiment {
    operator fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        savedPaymentMethods: List<PaymentMethod>,
        integrationConfiguration: PaymentElementLoader.Configuration,
        defaultPaymentSelection: PaymentSelection?,
    ): Boolean
}

internal class DefaultLogCardArtExperiment @Inject constructor(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
) : LogCardArtExperiment {

    override fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        savedPaymentMethods: List<PaymentMethod>,
        integrationConfiguration: PaymentElementLoader.Configuration,
        defaultPaymentSelection: PaymentSelection?,
    ): Boolean {
        val experimentsData = elementsSession.experimentsData
            ?: return FeatureFlags.enableCardArt.isEnabled

        val variant = experimentsData.experimentAssignments[ExperimentAssignment.OCS_MOBILE_CARD_ART]
            ?: return FeatureFlags.enableCardArt.isEnabled

        val savedCardPaymentMethods = savedPaymentMethods.filter { it.type == PaymentMethod.Type.Card }

        val selectedPaymentMethodHasCardArt = (defaultPaymentSelection as? PaymentSelection.Saved)
            ?.paymentMethod
            ?.card
            ?.cardArt
            ?.artImage != null

        eventReporter.onExperimentExposure(
            LoggableExperiment.OcsMobileCardArt(
                experimentsData = experimentsData,
                experiment = ExperimentAssignment.OCS_MOBILE_CARD_ART,
                group = variant,
                paymentMethodMetadata = paymentMethodMetadata,
                mode = mode,
                layout = integrationConfiguration.layoutDimensionValue(),
                savedPaymentMethodCount = savedPaymentMethods.size,
                savedCardPaymentMethodCount = savedCardPaymentMethods.size,
                savedCardPaymentMethodWithCardArtCount = savedCardPaymentMethods.count { it.card?.cardArt != null },
                selectedPaymentMethodType = defaultPaymentSelection?.paymentMethodType,
                selectedPaymentMethodHasCardArt = selectedPaymentMethodHasCardArt,
            )
        )

        return variant == TREATMENT
    }

    private companion object {
        const val TREATMENT = "treatment"
    }
}

private fun PaymentElementLoader.Configuration.layoutDimensionValue(): String {
    return when (this) {
        is PaymentElementLoader.Configuration.PaymentSheet -> when (configuration.paymentMethodLayout) {
            PaymentSheet.PaymentMethodLayout.Horizontal -> "horizontal"
            PaymentSheet.PaymentMethodLayout.Vertical -> "vertical"
            PaymentSheet.PaymentMethodLayout.Automatic -> "horizontal"
        }
        is PaymentElementLoader.Configuration.Embedded -> "vertical"
        is PaymentElementLoader.Configuration.CryptoOnramp -> "vertical"
    }
}

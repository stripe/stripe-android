package com.stripe.android.common.analytics.experiment

import com.stripe.android.common.di.MOBILE_SESSION_ID
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment.CONNECTIONS_FC_LITE_VS_NATIVE
import com.stripe.android.model.ElementsSession.ExperimentAssignment.CONNECTIONS_FC_LITE_VS_NATIVE_AA
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.payments.financialconnections.GetDefaultFinancialConnectionsAvailability
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsSdkAvailable
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject
import javax.inject.Named

internal interface LogFcLiteExperiment {
    operator fun invoke(elementsSession: ElementsSession)
}

internal class DefaultLogFcLiteExperiment internal constructor(
    private val eventReporter: EventReporter,
    private val mobileSessionId: String,
    isFullSdkAvailable: IsFinancialConnectionsSdkAvailable,
) : LogFcLiteExperiment {

    @Inject
    constructor(
        eventReporter: EventReporter,
        @Named(MOBILE_SESSION_ID) mobileSessionId: String,
    ) : this(eventReporter, mobileSessionId, DefaultIsFinancialConnectionsAvailable)

    private val fcSdkAvailability: String = when (GetDefaultFinancialConnectionsAvailability(isFullSdkAvailable)) {
        FinancialConnectionsAvailability.Full -> "FULL"
        FinancialConnectionsAvailability.Lite -> "LITE"
    }

    override fun invoke(elementsSession: ElementsSession) {
        val experimentsData = elementsSession.experimentsData ?: return

        listOf(CONNECTIONS_FC_LITE_VS_NATIVE, CONNECTIONS_FC_LITE_VS_NATIVE_AA).forEach { assignment ->
            val group = experimentsData.experimentAssignments[assignment] ?: return@forEach
            eventReporter.onExperimentExposure(
                experiment = LoggableExperiment.ConnectionsFCLiteVsNative(
                    arbId = experimentsData.arbId,
                    group = group,
                    experiment = assignment,
                    elementsSessionId = elementsSession.elementsSessionId,
                    mobileSessionId = mobileSessionId,
                    mobileSdkVersion = StripeSdkVersion.VERSION_NAME,
                    fcSdkAvailability = fcSdkAvailability,
                    availableLpms = elementsSession.orderedPaymentMethodTypesAndWallets.joinToString(","),
                )
            )
        }
    }
}

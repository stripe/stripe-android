package com.stripe.android.common.nfcscan

import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.common.nfcscan.security.IsDeviceSecureForNfc
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

internal interface IsNfcScanningAvailable {
    fun get(metadata: PaymentMethodMetadata): Boolean
}

internal class DefaultIsNfcScanningAvailable @Inject constructor(
    private val isDeviceSecureForNfc: IsDeviceSecureForNfc,
    private val nfcHardwareDelegate: NfcHardwareDelegate,
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
) : IsNfcScanningAvailable {
    override fun get(metadata: PaymentMethodMetadata): Boolean {
        val hasRequirements = metadata.isNfcScanningEnabled && !metadata.isTapToAddSupported

        if (!hasRequirements) {
            return false
        }

        val variant = metadata.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_NFC_SCANNING_FEATURE_HOLDBACK
        ]

        logExposureIfNeeded(variant, metadata)

        val canUseNfcScanning = variant == "treatment" || variant == null

        return canUseNfcScanning &&
            isDeviceSecureForNfc.get() &&
            nfcHardwareDelegate.isAvailable()
    }

    private fun logExposureIfNeeded(
        variant: String?,
        metadata: PaymentMethodMetadata,
    ) {
        if (variant == null) {
            return
        }

        val experimentsData = metadata.experimentsData ?: return
        val exposure = LoggableExperiment.OcsMobileNfcScanningFeatureHoldback(
            experimentsData = experimentsData,
            group = variant,
            metadata = metadata,
            mode = mode,
        )

        eventReporter.onExperimentExposure(exposure)
    }
}

internal class NoOpIsNfcScanningAvailable @Inject constructor() : IsNfcScanningAvailable {
    override fun get(metadata: PaymentMethodMetadata): Boolean = false
}

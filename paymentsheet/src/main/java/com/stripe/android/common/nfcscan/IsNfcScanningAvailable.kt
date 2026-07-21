package com.stripe.android.common.nfcscan

import android.util.Log
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

        val canUseNfcScanner = isDeviceSecureForNfc.get() &&
            nfcHardwareDelegate.isAvailable()

        val variant = metadata.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_NFC_SCANNING_FEATURE_HOLDBACK
        ]

        logExposureIfNeeded(variant, metadata, canUseNfcScanner)

        val canUseNfcScanning = variant == "treatment" || variant == null

        return canUseNfcScanning && canUseNfcScanner
    }

    private fun logExposureIfNeeded(
        variant: String?,
        metadata: PaymentMethodMetadata,
        canUseNfcScanner: Boolean,
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
            canUseNfcScanner = canUseNfcScanner,
        )

        eventReporter.onExperimentExposure(exposure)

        val filteredDimensions = exposure.dimensions.filterKeys { key ->
            key !in arrayOf(
                "displayed_payment_method_types",
                "displayed_payment_method_types_including_wallets",
                "in_app_elements_integration_type"
            )
        }

        Log.d("NFC-Scan-Experiment", "variant=$variant, dimensions=${filteredDimensions}")
    }
}

internal class NoOpIsNfcScanningAvailable @Inject constructor() : IsNfcScanningAvailable {
    override fun get(metadata: PaymentMethodMetadata): Boolean = false
}

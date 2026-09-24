package com.stripe.android.common.nfcscan

import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.common.nfcscan.security.IsDeviceSecureForNfc
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.cardscan.IsStripeCardScanAvailable
import javax.inject.Inject

internal interface IsNfcScanningAvailable {
    fun get(metadata: PaymentMethodMetadata): NfcScanningAvailability
}

internal sealed interface NfcScanningAvailability {
    data object Unavailable : NfcScanningAvailability

    data class Available(
        val shouldBePrimaryScanningOption: Boolean,
    ) : NfcScanningAvailability
}

internal class DefaultIsNfcScanningAvailable @Inject constructor(
    private val isDeviceSecureForNfc: IsDeviceSecureForNfc,
    private val nfcHardwareDelegate: NfcHardwareDelegate,
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
    private val isStripeCardScanAvailable: IsStripeCardScanAvailable,
) : IsNfcScanningAvailable {
    override fun get(metadata: PaymentMethodMetadata): NfcScanningAvailability {
        if (!metadata.isNfcScanningEnabled) {
            return NfcScanningAvailability.Unavailable
        }

        val isNfcHardwareAvailable = nfcHardwareDelegate.isAvailable()
        val canUseNfcScanner = isDeviceSecureForNfc.get() && isNfcHardwareAvailable

        val variant = metadata.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_NFC_SCANNING_FEATURE_HOLDBACK
        ]

        logExposureIfNeeded(variant, metadata, canUseNfcScanner)

        if (!isNfcHardwareAvailable) {
            return NfcScanningAvailability.Unavailable
        }

        return when (variant) {
            "treatment" -> NfcScanningAvailability.Available(shouldBePrimaryScanningOption = true)
            null -> NfcScanningAvailability.Available(shouldBePrimaryScanningOption = false)
            else -> NfcScanningAvailability.Unavailable
        }
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
    }

    private fun canUseStripeCardScan(
        metadata: PaymentMethodMetadata,
    ): Boolean {
        return metadata.isStripeCardScanAllowed && isStripeCardScanAvailable()
    }
}

internal class NoOpIsNfcScanningAvailable @Inject constructor() : IsNfcScanningAvailable {
    override fun get(metadata: PaymentMethodMetadata): NfcScanningAvailability {
        return NfcScanningAvailability.Unavailable
    }
}

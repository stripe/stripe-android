package com.stripe.android.common.nfcscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.nfcscan.hardware.FakeNfcHardwareDelegate
import com.stripe.android.common.nfcscan.security.FakeIsDeviceSecureForNfc
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IsNfcScanningAvailableTest {
    @Test
    fun `returns false when NFC scanning is disabled on metadata`() {
        val isNfcScanningAvailable = createIsNfcScanningAvailable()

        assertThat(
            isNfcScanningAvailable.get(
                metadata = createMetadata(isNfcScanningEnabled = false),
            )
        ).isFalse()
    }

    @Test
    fun `returns false when tap to add is supported`() {
        val isNfcScanningAvailable = createIsNfcScanningAvailable()

        assertThat(
            isNfcScanningAvailable.get(
                metadata = createMetadata(
                    isNfcScanningEnabled = true,
                    isTapToAddSupported = true,
                ),
            )
        ).isFalse()
    }

    @Test
    fun `returns false when device is not secure`() {
        val isNfcScanningAvailable = createIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = false),
        )

        assertThat(
            isNfcScanningAvailable.get(
                metadata = createMetadata(isNfcScanningEnabled = true),
            )
        ).isFalse()
    }

    @Test
    fun `returns false when NFC hardware is unavailable`() {
        val isNfcScanningAvailable = createIsNfcScanningAvailable(
            nfcHardwareDelegate = FakeNfcHardwareDelegate(result = false),
        )

        assertThat(
            isNfcScanningAvailable.get(
                metadata = createMetadata(isNfcScanningEnabled = true),
            )
        ).isFalse()
    }

    @Test
    fun `returns false when assigned to control`() {
        val isNfcScanningAvailable = createIsNfcScanningAvailable()

        assertThat(
            isNfcScanningAvailable.get(
                metadata = createMetadata(
                    isNfcScanningEnabled = true,
                    experimentVariant = "control",
                ),
            )
        ).isFalse()
    }

    @Test
    fun `returns true when assigned to treatment and NFC is enabled and secure`() {
        val isNfcScanningAvailable = createIsNfcScanningAvailable()

        assertThat(
            isNfcScanningAvailable.get(
                metadata = createMetadata(
                    isNfcScanningEnabled = true,
                    experimentVariant = "treatment",
                ),
            )
        ).isTrue()
    }

    @Test
    fun `returns true when experiment is unassigned and NFC is enabled and secure`() {
        val isNfcScanningAvailable = createIsNfcScanningAvailable()

        assertThat(
            isNfcScanningAvailable.get(
                metadata = createMetadata(isNfcScanningEnabled = true),
            )
        ).isTrue()
    }

    @Test
    fun `logs experiment exposure when assigned to treatment`() = runTest {
        val eventReporter = FakeEventReporter()
        val isNfcScanningAvailable = createIsNfcScanningAvailable(eventReporter = eventReporter)
        val metadata = createMetadata(
            isNfcScanningEnabled = true,
            experimentVariant = "treatment",
        )

        isNfcScanningAvailable.get(metadata)

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.OcsMobileNfcScanningFeatureHoldback
        assertThat(exposure.group).isEqualTo("treatment")
        assertThat(exposure.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_NFC_SCANNING_FEATURE_HOLDBACK)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `logs experiment exposure when assigned to control`() = runTest {
        val eventReporter = FakeEventReporter()
        val isNfcScanningAvailable = createIsNfcScanningAvailable(eventReporter = eventReporter)
        val metadata = createMetadata(
            isNfcScanningEnabled = true,
            experimentVariant = "control",
        )

        isNfcScanningAvailable.get(metadata)

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.OcsMobileNfcScanningFeatureHoldback
        assertThat(exposure.group).isEqualTo("control")
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `does not log experiment exposure when experiment is unassigned`() = runTest {
        val eventReporter = FakeEventReporter()
        val isNfcScanningAvailable = createIsNfcScanningAvailable(eventReporter = eventReporter)

        isNfcScanningAvailable.get(createMetadata(isNfcScanningEnabled = true))

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    private fun createIsNfcScanningAvailable(
        isDeviceSecureForNfc: FakeIsDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
        nfcHardwareDelegate: FakeNfcHardwareDelegate = FakeNfcHardwareDelegate(result = true),
        eventReporter: FakeEventReporter = FakeEventReporter(),
        mode: EventReporter.Mode = EventReporter.Mode.Complete,
    ): DefaultIsNfcScanningAvailable {
        return DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = isDeviceSecureForNfc,
            nfcHardwareDelegate = nfcHardwareDelegate,
            eventReporter = eventReporter,
            mode = mode,
        )
    }

    private fun createMetadata(
        isNfcScanningEnabled: Boolean,
        isTapToAddSupported: Boolean = false,
        experimentVariant: String? = null,
    ) = PaymentMethodMetadataFactory.create(
        isNfcScanningEnabled = isNfcScanningEnabled,
        isTapToAddSupported = isTapToAddSupported,
        experimentsData = experimentVariant?.let { variant ->
            ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    ExperimentAssignment.OCS_MOBILE_NFC_SCANNING_FEATURE_HOLDBACK to variant,
                ),
            )
        },
    )
}

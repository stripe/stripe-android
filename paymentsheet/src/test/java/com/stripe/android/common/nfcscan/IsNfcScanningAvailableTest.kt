package com.stripe.android.common.nfcscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.security.FakeIsDeviceSecureForNfc
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.state.FakeTapToAddAvailabilityFactory
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IsNfcScanningAvailableTest {
    private val elementsSession = mock<ElementsSession>()
    private val customerMetadata = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA

    @After
    fun tearDown() {
        FeatureFlags.enableNfcScanning.reset()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when NFC feature flag is disabled`() {
        FeatureFlags.enableNfcScanning.setEnabled(false)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
        )

        assertThat(isNfcScanningAvailable.get(elementsSession, customerMetadata)).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when tap to add is available`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = true),
        )

        assertThat(isNfcScanningAvailable.get(elementsSession, customerMetadata)).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when device is not sure`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = false),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
        )

        assertThat(isNfcScanningAvailable.get(elementsSession, customerMetadata)).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns true when NFC flag on, tap to add unavailable, and secure`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
        )

        assertThat(isNfcScanningAvailable.get(elementsSession, customerMetadata)).isTrue()
    }
}

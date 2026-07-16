package com.stripe.android.common.nfcscan

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.hardware.FakeNfcHardwareDelegate
import com.stripe.android.common.nfcscan.security.FakeIsDeviceSecureForNfc
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.state.FakeTapToAddAvailabilityFactory
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IsNfcScanningAvailableTest {
    private val customerMetadata = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA

    @After
    fun tearDown() {
        FeatureFlags.enableNfcScanning.reset()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when local NFC feature flag is disabled`() {
        FeatureFlags.enableNfcScanning.setEnabled(false)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
            nfcHardwareDelegate = FakeNfcHardwareDelegate(result = true),
        )

        assertThat(
            isNfcScanningAvailable.get(
                elementsSession = createElementsSession(isNfcScanningEnabled = true),
                customerMetadata = customerMetadata,
            )
        ).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when remote feature flag is disabled`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
            nfcHardwareDelegate = FakeNfcHardwareDelegate(result = true),
        )

        assertThat(
            isNfcScanningAvailable.get(
                elementsSession = createElementsSession(isNfcScanningEnabled = false),
                customerMetadata = customerMetadata,
            )
        ).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when tap to add is available`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = true),
            nfcHardwareDelegate = FakeNfcHardwareDelegate(result = true),
        )

        assertThat(
            isNfcScanningAvailable.get(
                elementsSession = createElementsSession(isNfcScanningEnabled = true),
                customerMetadata = customerMetadata,
            )
        ).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when device is not sure`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = false),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
            nfcHardwareDelegate = FakeNfcHardwareDelegate(result = true),
        )

        assertThat(
            isNfcScanningAvailable.get(
                elementsSession = createElementsSession(isNfcScanningEnabled = true),
                customerMetadata = customerMetadata,
            )
        ).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns false when NFC hardware is unavailable`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
            nfcHardwareDelegate = FakeNfcHardwareDelegate(result = false),
        )

        assertThat(
            isNfcScanningAvailable.get(
                elementsSession = createElementsSession(isNfcScanningEnabled = true),
                customerMetadata = customerMetadata,
            )
        ).isFalse()
    }

    @Test
    fun `IsNfcScanningAvailableForPaymentElement returns true when flag on, TTA off, and NFC enabled & secure`() {
        FeatureFlags.enableNfcScanning.setEnabled(true)

        val isNfcScanningAvailable = DefaultIsNfcScanningAvailable(
            isDeviceSecureForNfc = FakeIsDeviceSecureForNfc(result = true),
            tapToAddAvailabilityFactory = FakeTapToAddAvailabilityFactory(isAvailableResult = false),
            nfcHardwareDelegate = FakeNfcHardwareDelegate(result = true),
        )

        assertThat(
            isNfcScanningAvailable.get(
                elementsSession = createElementsSession(isNfcScanningEnabled = true),
                customerMetadata = customerMetadata,
            )
        ).isTrue()
    }

    private fun createElementsSession(isNfcScanningEnabled: Boolean): ElementsSession {
        return ElementsSession(
            linkSettings = null,
            paymentMethodSpecs = null,
            externalPaymentMethodData = null,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            orderedPaymentMethodTypesAndWallets = emptyList(),
            flags = mapOf(
                ElementsSession.Flag.ELEMENTS_MOBILE_ANDROID_NFC_SCANNING_ENABLED to isNfcScanningEnabled,
            ),
            experimentsData = null,
            customer = null,
            merchantCountry = null,
            merchantLogoUrl = null,
            cardBrandChoice = null,
            isGooglePayEnabled = false,
            sessionsError = null,
            customPaymentMethods = emptyList(),
            elementsSessionId = "elements_session_test",
            passiveCaptcha = null,
            elementsSessionConfigId = null,
            accountId = null,
            merchantId = null,
        )
    }
}

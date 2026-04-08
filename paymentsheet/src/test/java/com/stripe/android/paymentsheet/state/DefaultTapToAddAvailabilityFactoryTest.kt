package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddConnectionManager
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.utils.FakeElementsSessionRepository.Companion.DEFAULT_ELEMENTS_SESSION_CONFIG_ID
import com.stripe.android.utils.FakeElementsSessionRepository.Companion.DEFAULT_ELEMENTS_SESSION_ID
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultTapToAddAvailabilityFactoryTest {
    @Test
    fun `isAvailable is true when supported by connection manager, session flag on, and customer metadata present`() {
        val factory = DefaultTapToAddAvailabilityFactory(
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true),
        )

        assertThat(
            factory.isAvailable(
                elementsSession = elementsSession(
                    flags = mapOf(
                        ElementsSession.Flag.ELEMENTS_MOBILE_ANDROID_TAP_TO_ADD_ENABLED to true,
                    ),
                ),
                customerMetadata = DEFAULT_CUSTOMER_METADATA,
            )
        ).isTrue()
    }

    @Test
    fun `isAvailable is false when unsupported by connection manager`() {
        val factory = DefaultTapToAddAvailabilityFactory(
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = false),
        )

        assertThat(
            factory.isAvailable(
                elementsSession = elementsSession(
                    flags = mapOf(
                        ElementsSession.Flag.ELEMENTS_MOBILE_ANDROID_TAP_TO_ADD_ENABLED to true,
                    ),
                ),
                customerMetadata = DEFAULT_CUSTOMER_METADATA,
            )
        ).isFalse()
    }

    @Test
    fun `isAvailable is false when elements session disables tap to add`() {
        val factory = DefaultTapToAddAvailabilityFactory(
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true),
        )

        assertThat(
            factory.isAvailable(
                elementsSession = elementsSession(
                    flags = mapOf(
                        ElementsSession.Flag.ELEMENTS_MOBILE_ANDROID_TAP_TO_ADD_ENABLED to false,
                    ),
                ),
                customerMetadata = DEFAULT_CUSTOMER_METADATA,
            )
        ).isFalse()
    }

    @Test
    fun `isAvailable is false when customer metadata is null`() {
        val factory = DefaultTapToAddAvailabilityFactory(
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true),
        )

        assertThat(
            factory.isAvailable(
                elementsSession = elementsSession(
                    flags = mapOf(
                        ElementsSession.Flag.ELEMENTS_MOBILE_ANDROID_TAP_TO_ADD_ENABLED to true,
                    ),
                ),
                customerMetadata = null,
            )
        ).isFalse()
    }

    private fun elementsSession(
        flags: Map<ElementsSession.Flag, Boolean>,
    ): ElementsSession {
        return ElementsSession(
            linkSettings = null,
            paymentMethodSpecs = null,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            merchantCountry = null,
            isGooglePayEnabled = true,
            sessionsError = null,
            externalPaymentMethodData = null,
            customer = null,
            cardBrandChoice = null,
            customPaymentMethods = emptyList(),
            elementsSessionId = DEFAULT_ELEMENTS_SESSION_ID,
            flags = flags,
            orderedPaymentMethodTypesAndWallets = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes,
            experimentsData = null,
            passiveCaptcha = null,
            merchantLogoUrl = null,
            elementsSessionConfigId = DEFAULT_ELEMENTS_SESSION_CONFIG_ID,
            accountId = "acct_test",
            merchantId = "acct_test",
        )
    }
}

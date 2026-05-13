package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddConnectionManager
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.utils.FakeElementsSessionRepository.Companion.DEFAULT_ELEMENTS_SESSION_CONFIG_ID
import com.stripe.android.utils.FakeElementsSessionRepository.Companion.DEFAULT_ELEMENTS_SESSION_ID
import com.stripe.android.utils.PaymentElementCallbackTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(TapToAddPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultTapToAddAvailabilityFactoryTest {
    @get:Rule
    val paymentElementCallbackTestRule = PaymentElementCallbackTestRule()

    @Test
    fun `isAvailable is true when supported by connection manager, session flag on, and customer metadata present`() {
        PaymentElementCallbackReferences[CALLBACK_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createCardPresentSetupIntentCallback { CreateIntentResult.Success("seti_test_secret") }
            .build()

        val factory = DefaultTapToAddAvailabilityFactory(
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true),
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
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
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
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
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
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
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
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

    @Test
    fun `isAvailable is false when createCardPresentSetupIntentCallback is not registered`() {
        val factory = DefaultTapToAddAvailabilityFactory(
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true),
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
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

    private companion object {
        private const val CALLBACK_IDENTIFIER = "DefaultTapToAddAvailabilityFactoryTest"
    }
}

package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class ElementsSessionLoadSessionAndCustomerInfoTest {

    @Test
    fun `retrieves elements session from repository`() = runScenario {
        val result = loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = null,
        )

        assertThat(result.elementsSession.stripeIntent)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
    }

    @Test
    fun `passes savedPaymentMethodSelection to repository`() = runScenario {
        val savedSelection = SavedSelection.PaymentMethod(id = "pm_123")

        loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = savedSelection,
        )

        assertThat(elementsSessionRepository.lastParams?.savedPaymentMethodSelectionId)
            .isEqualTo("pm_123")
    }

    @Test
    fun `returns null customer info when no customer configured`() = runScenario {
        val result = loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = null,
        )

        assertThat(result.customerInfo).isNull()
    }

    @Test
    fun `returns CustomerSession when customer session configured`() = runScenario(
        sessionsCustomer = ElementsSession.Customer(
            paymentMethods = PaymentMethodFactory.cards(1),
            session = ElementsSession.Customer.Session(
                id = "cuss_1",
                customerId = "cus_1",
                liveMode = false,
                apiKey = "ek_123",
                apiKeyExpiry = 555555555,
                components = ElementsSession.Customer.Components(
                    mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                ),
            ),
            defaultPaymentMethod = null,
        ),
    ) {
        val result = loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant",
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "cuss_1",
                ),
            ).asCommonConfiguration(),
            savedPaymentMethodSelection = null,
        )

        assertThat(result.customerInfo).isInstanceOf<CustomerInfo.CustomerSession>()
        val customerSession = result.customerInfo as CustomerInfo.CustomerSession
        assertThat(customerSession.id).isEqualTo("cus_1")
        assertThat(customerSession.customerSessionClientSecret).isEqualTo("cuss_1")
    }

    @Test
    fun `returns Legacy when legacy ephemeral key configured`() = runScenario {
        val result = loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_1",
                    ephemeralKeySecret = "ek_123",
                ),
            ).asCommonConfiguration(),
            savedPaymentMethodSelection = null,
        )

        assertThat(result.customerInfo).isInstanceOf<CustomerInfo.Legacy>()
        val legacy = result.customerInfo as CustomerInfo.Legacy
        assertThat(legacy.id).isEqualTo("cus_1")
        assertThat(legacy.ephemeralKeySecret).isEqualTo("ek_123")
    }

    @Test
    fun `throws in test mode when CustomerSession configured but no customer in elements session`() = runTest {
        val errorReporter = FakeErrorReporter()
        val loader = ElementsSessionLoadSessionAndCustomerInfo(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(isLiveMode = false),
                error = null,
                linkSettings = null,
                sessionsCustomer = null,
            ),
            errorReporter = errorReporter,
        )

        val exception = assertFailsWith<IllegalStateException> {
            loader(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_1",
                    ),
                ).asCommonConfiguration(),
                savedPaymentMethodSelection = null,
            )
        }

        assertThat(exception.message).contains("Excepted 'customer' attribute")

        assertThat(errorReporter.getLoggedErrors()).contains(
            ErrorReporter.UnexpectedErrorEvent
                .PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND
                .eventName
        )
    }

    @Test
    fun `returns null in live mode when CustomerSession configured but no customer in elements session`() = runTest {
        val errorReporter = FakeErrorReporter()
        val loader = ElementsSessionLoadSessionAndCustomerInfo(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(isLiveMode = true),
                error = null,
                linkSettings = null,
                sessionsCustomer = null,
            ),
            errorReporter = errorReporter,
        )

        val result = loader(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant",
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "cuss_1",
                ),
            ).asCommonConfiguration(),
            savedPaymentMethodSelection = null,
        )

        assertThat(result.customerInfo).isNull()

        assertThat(errorReporter.getLoggedErrors()).contains(
            ErrorReporter.UnexpectedErrorEvent
                .PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND
                .eventName
        )
    }

    private data class Scenario(
        val loader: ElementsSessionLoadSessionAndCustomerInfo,
        val elementsSessionRepository: FakeElementsSessionRepository,
    )

    private fun runScenario(
        sessionsCustomer: ElementsSession.Customer? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            error = null,
            linkSettings = null,
            sessionsCustomer = sessionsCustomer,
        )
        val loader = ElementsSessionLoadSessionAndCustomerInfo(
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = FakeErrorReporter(),
        )
        Scenario(
            loader = loader,
            elementsSessionRepository = elementsSessionRepository,
        ).block()
    }

    private companion object {
        private val DEFAULT_CONFIG = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
        ).asCommonConfiguration()
    }
}

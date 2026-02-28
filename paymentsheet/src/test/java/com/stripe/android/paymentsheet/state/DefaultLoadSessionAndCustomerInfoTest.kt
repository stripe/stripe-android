package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.FakeCheckoutSessionRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class DefaultLoadSessionAndCustomerInfoTest {

    @Test
    fun `retrieveCheckoutSession returns null for non-CheckoutSession modes`() = runScenario {
        val result = loadSessionAndCustomerInfo(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = null,
        )

        assertThat(result.checkoutSession).isNull()
        assertThat(result.elementsSession).isNotNull()
    }

    @Test
    fun `retrieveCheckoutSession returns response for CheckoutSession mode`() = runScenario(
        checkoutSessionRepository = FakeCheckoutSessionRepository(
            initResult = Result.success(CHECKOUT_SESSION_RESPONSE),
        ),
    ) {
        val result = loadSessionAndCustomerInfo(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                clientSecret = "cs_test_123_secret_abc",
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = null,
        )

        assertThat(result.checkoutSession).isEqualTo(CHECKOUT_SESSION_RESPONSE)
        assertThat(result.elementsSession).isEqualTo(CHECKOUT_SESSION_RESPONSE.elementsSession)
    }

    @Test
    fun `throws when CheckoutSession response has no elementsSession`() = runScenario(
        checkoutSessionRepository = FakeCheckoutSessionRepository(
            initResult = Result.success(
                CHECKOUT_SESSION_RESPONSE.copy(elementsSession = null),
            ),
        ),
    ) {
        assertFailsWith<IllegalStateException> {
            loadSessionAndCustomerInfo(
                initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                    clientSecret = "cs_test_123_secret_abc",
                ),
                configuration = DEFAULT_CONFIG,
                savedPaymentMethodSelection = null,
            )
        }
    }

    @Test
    fun `retrieves elements session from repository for PaymentIntent mode`() = runScenario {
        val result = loadSessionAndCustomerInfo(
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
    fun `passes savedPaymentMethodSelection to elements session repository`() = runScenario {
        val savedSelection = SavedSelection.PaymentMethod(id = "pm_123")

        loadSessionAndCustomerInfo(
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
    fun `createCustomerInfo returns null when no customer configured`() = runScenario {
        val result = loadSessionAndCustomerInfo(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant",
            ).asCommonConfiguration(),
            savedPaymentMethodSelection = null,
        )

        assertThat(result.customerInfo).isNull()
    }

    @Test
    fun `createCustomerInfo returns CustomerSession when customer session configured`() = runScenario(
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
        val result = loadSessionAndCustomerInfo(
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
    fun `createCustomerInfo returns Legacy when legacy ephemeral key configured`() = runScenario {
        val result = loadSessionAndCustomerInfo(
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
    fun `createCustomerInfo returns CheckoutSession when checkout session has customer`() = runScenario(
        checkoutSessionRepository = FakeCheckoutSessionRepository(
            initResult = Result.success(CHECKOUT_SESSION_RESPONSE),
        ),
    ) {
        val result = loadSessionAndCustomerInfo(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                clientSecret = "cs_test_123_secret_abc",
            ),
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant",
            ).asCommonConfiguration(),
            savedPaymentMethodSelection = null,
        )

        assertThat(result.customerInfo).isInstanceOf<CustomerInfo.CheckoutSession>()
        val checkoutCustomer = result.customerInfo as CustomerInfo.CheckoutSession
        assertThat(checkoutCustomer.customer.id).isEqualTo("cus_test_123")
    }

    @Test
    fun `createCustomerInfo throws in test mode when CustomerSession configured but no customer in elements session`() =
        runScenario {
            val errorReporter = FakeErrorReporter()
            val loader = DefaultLoadSessionAndCustomerInfo(
                checkoutSessionRepository = FakeCheckoutSessionRepository(),
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
    fun `createCustomerInfo returns null in live mode when CustomerSession configured but no customer in elements session`() =
        runScenario {
            val errorReporter = FakeErrorReporter()
            val loader = DefaultLoadSessionAndCustomerInfo(
                checkoutSessionRepository = FakeCheckoutSessionRepository(),
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
        val loadSessionAndCustomerInfo: DefaultLoadSessionAndCustomerInfo,
        val elementsSessionRepository: FakeElementsSessionRepository,
    )

    private fun runScenario(
        checkoutSessionRepository: FakeCheckoutSessionRepository = FakeCheckoutSessionRepository(),
        sessionsCustomer: ElementsSession.Customer? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            error = null,
            linkSettings = null,
            sessionsCustomer = sessionsCustomer,
        )
        val loader = DefaultLoadSessionAndCustomerInfo(
            checkoutSessionRepository = checkoutSessionRepository,
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = FakeErrorReporter(),
        )
        Scenario(
            loadSessionAndCustomerInfo = loader,
            elementsSessionRepository = elementsSessionRepository,
        ).block()
    }

    private companion object {
        private val DEFAULT_CONFIG = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
        ).asCommonConfiguration()

        private val CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponse(
            id = "cs_test_123",
            amount = 5099,
            currency = "usd",
            elementsSession = ElementsSession(
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
                elementsSessionId = "es_123",
                flags = emptyMap(),
                orderedPaymentMethodTypesAndWallets = listOf("card"),
                experimentsData = null,
                passiveCaptcha = null,
                merchantLogoUrl = null,
                elementsSessionConfigId = "config_123",
                accountId = "acct_123",
                merchantId = "acct_123",
            ),
            customer = CheckoutSessionResponse.Customer(
                id = "cus_test_123",
                paymentMethods = PaymentMethodFactory.cards(2),
                canDetachPaymentMethod = true,
            ),
        )
    }
}

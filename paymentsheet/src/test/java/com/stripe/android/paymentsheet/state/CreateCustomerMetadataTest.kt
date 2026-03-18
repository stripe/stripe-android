package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class CreateCustomerMetadataTest {

    @Test
    fun `checkout session with customer returns CheckoutSession metadata`() = runScenario(
        initializationMode = checkoutSessionInitMode(
            customer = CHECKOUT_CUSTOMER,
        ),
    ) {
        val result = result as CustomerMetadata.CheckoutSession
        assertThat(result.sessionId).isEqualTo("cs_test_123")
        assertThat(result.customerId).isEqualTo("cus_checkout_1")
    }

    @Test
    fun `checkout session without customer returns null`() = runScenario(
        initializationMode = checkoutSessionInitMode(customer = null),
    ) {
        assertThat(result).isNull()
    }

    @Test
    fun `checkout session with canDetachPaymentMethod true returns Full remove permission`() = runScenario(
        initializationMode = checkoutSessionInitMode(
            customer = CHECKOUT_CUSTOMER.copy(canDetachPaymentMethod = true),
        ),
    ) {
        val result = result as CustomerMetadata.CheckoutSession
        assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Full)
    }

    @Test
    fun `checkout session with canDetachPaymentMethod false returns None remove permission`() = runScenario(
        initializationMode = checkoutSessionInitMode(
            customer = CHECKOUT_CUSTOMER.copy(canDetachPaymentMethod = false),
        ),
    ) {
        val result = result as CustomerMetadata.CheckoutSession
        assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.None)
    }

    @Test
    fun `checkout session with offerSave enabled returns Enabled save consent`() = runScenario(
        initializationMode = checkoutSessionInitMode(
            customer = CHECKOUT_CUSTOMER,
            savedPaymentMethodsOfferSave = CheckoutSessionResponse.SavedPaymentMethodsOfferSave(
                enabled = true,
                status = CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED,
            ),
        ),
    ) {
        val result = result as CustomerMetadata.CheckoutSession
        assertThat(result.saveConsent).isEqualTo(PaymentMethodSaveConsentBehavior.Enabled)
    }

    @Test
    fun `checkout session with offerSave disabled returns Disabled save consent`() = runScenario(
        initializationMode = checkoutSessionInitMode(
            customer = CHECKOUT_CUSTOMER,
            savedPaymentMethodsOfferSave = CheckoutSessionResponse.SavedPaymentMethodsOfferSave(
                enabled = false,
                status = CheckoutSessionResponse.SavedPaymentMethodsOfferSave.Status.NOT_ACCEPTED,
            ),
        ),
    ) {
        val result = result as CustomerMetadata.CheckoutSession
        assertThat(result.saveConsent).isEqualTo(
            PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null)
        )
    }

    @Test
    fun `checkout session without offerSave returns Disabled save consent`() = runScenario(
        initializationMode = checkoutSessionInitMode(
            customer = CHECKOUT_CUSTOMER,
            savedPaymentMethodsOfferSave = null,
        ),
    ) {
        val result = result as CustomerMetadata.CheckoutSession
        assertThat(result.saveConsent).isEqualTo(
            PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null)
        )
    }

    @Test
    fun `customer session with customer in elements session returns CustomerSession metadata`() = runScenario(
        configuration = CONFIG_CUSTOMER_SESSION,
        elementsSession = createElementsSession(customer = ELEMENTS_SESSION_CUSTOMER),
    ) {
        val result = result as CustomerMetadata.CustomerSession
        assertThat(result.id).isEqualTo("cus_1")
        assertThat(result.ephemeralKeySecret).isEqualTo("ek_test_1234")
        assertThat(result.customerSessionClientSecret).isEqualTo("css_test_123")
    }

    @Test
    fun `customer session without customer in elements session throws in test mode`() = runTest {
        val errorReporter = FakeErrorReporter()
        val createCustomerMetadata = CreateCustomerMetadata(errorReporter)

        assertFailsWith<IllegalStateException> {
            createCustomerMetadata(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "pi_123_secret_456",
                ),
                configuration = CONFIG_CUSTOMER_SESSION,
                elementsSession = createElementsSession(
                    customer = null,
                    isLiveMode = false,
                ),
            )
        }

        val call = errorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND
        )
    }

    @Test
    fun `customer session without customer in elements session returns null in live mode`() = runTest {
        val errorReporter = FakeErrorReporter()
        val createCustomerMetadata = CreateCustomerMetadata(errorReporter)

        val result = createCustomerMetadata(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_456",
            ),
            configuration = CONFIG_CUSTOMER_SESSION,
            elementsSession = createElementsSession(
                customer = null,
                isLiveMode = true,
            ),
        )

        assertThat(result).isNull()

        val call = errorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND
        )
    }

    @Test
    fun `customer session with default payment method enabled passes true`() = runScenario(
        configuration = CONFIG_CUSTOMER_SESSION,
        elementsSession = createElementsSession(
            customer = createElementsSessionCustomer(isPaymentMethodSetAsDefaultEnabled = true),
        ),
    ) {
        val result = result as CustomerMetadata.CustomerSession
        assertThat(result.isPaymentMethodSetAsDefaultEnabled).isTrue()
    }

    @Test
    fun `customer session with default payment method disabled passes false`() = runScenario(
        configuration = CONFIG_CUSTOMER_SESSION,
        elementsSession = createElementsSession(
            customer = createElementsSessionCustomer(isPaymentMethodSetAsDefaultEnabled = false),
        ),
    ) {
        val result = result as CustomerMetadata.CustomerSession
        assertThat(result.isPaymentMethodSetAsDefaultEnabled).isFalse()
    }

    @Test
    fun `legacy ephemeral key returns LegacyEphemeralKey metadata`() = runScenario(
        configuration = CONFIG_LEGACY_EK,
    ) {
        val result = result as CustomerMetadata.LegacyEphemeralKey
        assertThat(result.id).isEqualTo("cus_123")
        assertThat(result.ephemeralKeySecret).isEqualTo(PaymentSheetFixtures.DEFAULT_EPHEMERAL_KEY)
    }

    @Test
    fun `legacy ephemeral key with default payment method enabled passes true`() = runScenario(
        configuration = CONFIG_LEGACY_EK,
        elementsSession = createElementsSession(
            customer = createElementsSessionCustomer(isPaymentMethodSetAsDefaultEnabled = true),
        ),
    ) {
        val result = result as CustomerMetadata.LegacyEphemeralKey
        assertThat(result.isPaymentMethodSetAsDefaultEnabled).isTrue()
    }

    @Test
    fun `legacy ephemeral key without customer in elements session passes false for default`() = runScenario(
        configuration = CONFIG_LEGACY_EK,
        elementsSession = createElementsSession(customer = null),
    ) {
        val result = result as CustomerMetadata.LegacyEphemeralKey
        assertThat(result.isPaymentMethodSetAsDefaultEnabled).isFalse()
    }

    @Test
    fun `null customer configuration returns null`() = runScenario(
        configuration = CONFIG_NO_CUSTOMER,
    ) {
        assertThat(result).isNull()
    }

    private fun runScenario(
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123_secret_456"),
        configuration: CommonConfiguration = CONFIG_CUSTOMER_SESSION,
        elementsSession: ElementsSession = createElementsSession(customer = ELEMENTS_SESSION_CUSTOMER),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val errorReporter = FakeErrorReporter()
        val createCustomerMetadata = CreateCustomerMetadata(errorReporter)

        val result = createCustomerMetadata(
            initializationMode = initializationMode,
            configuration = configuration,
            elementsSession = elementsSession,
        )

        Scenario(
            result = result,
            errorReporter = errorReporter,
        ).block()

        errorReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        val result: CustomerMetadata?,
        val errorReporter: FakeErrorReporter,
    )

    private companion object {
        val CHECKOUT_CUSTOMER = CheckoutSessionResponse.Customer(
            id = "cus_checkout_1",
            paymentMethods = emptyList(),
            canDetachPaymentMethod = true,
        )

        val ELEMENTS_SESSION_CUSTOMER = createElementsSessionCustomer()

        val CONFIG_CUSTOMER_SESSION = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
            customer = PaymentSheet.CustomerConfiguration(
                id = "cus_1",
                ephemeralKeySecret = "ek_test_1234",
                accessType = PaymentSheet.CustomerAccessType.CustomerSession(
                    customerSessionClientSecret = "css_test_123",
                ),
            ),
        ).asCommonConfiguration()

        val CONFIG_LEGACY_EK = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration()

        val CONFIG_NO_CUSTOMER = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
        ).asCommonConfiguration()

        fun checkoutSessionInitMode(
            customer: CheckoutSessionResponse.Customer?,
            savedPaymentMethodsOfferSave: CheckoutSessionResponse.SavedPaymentMethodsOfferSave? = null,
        ): PaymentElementLoader.InitializationMode.CheckoutSession {
            return PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = "instances_test",
                checkoutSessionResponse = CheckoutSessionResponse(
                    id = "cs_test_123",
                    amount = 1000,
                    currency = "usd",
                    mode = CheckoutSessionResponse.Mode.PAYMENT,
                    customerEmail = null,
                    elementsSession = null,
                    paymentIntent = null,
                    setupIntent = null,
                    customer = customer,
                    savedPaymentMethodsOfferSave = savedPaymentMethodsOfferSave,
                    totalSummary = null,
                    lineItems = emptyList(),
                    shippingOptions = emptyList(),
                ),
            )
        }

        fun createElementsSessionCustomer(
            isPaymentMethodSetAsDefaultEnabled: Boolean = false,
        ): ElementsSession.Customer {
            return ElementsSession.Customer(
                paymentMethods = emptyList(),
                defaultPaymentMethod = null,
                session = ElementsSession.Customer.Session(
                    id = "cuss_123",
                    customerId = "cus_1",
                    liveMode = false,
                    apiKey = "ek_test_1234",
                    apiKeyExpiry = 999999999,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                            isPaymentMethodSaveEnabled = false,
                            paymentMethodRemove =
                                ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                            paymentMethodRemoveLast =
                                ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                            allowRedisplayOverride = null,
                            isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                        ),
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                    ),
                ),
            )
        }

        fun createElementsSession(
            customer: ElementsSession.Customer? = null,
            isLiveMode: Boolean = false,
        ): ElementsSession {
            val stripeIntent = if (isLiveMode) {
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            } else {
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            }

            return ElementsSession(
                stripeIntent = stripeIntent,
                linkSettings = null,
                paymentMethodSpecs = null,
                externalPaymentMethodData = null,
                orderedPaymentMethodTypesAndWallets = listOf("card"),
                flags = emptyMap(),
                experimentsData = null,
                customer = customer,
                merchantCountry = null,
                merchantLogoUrl = null,
                cardBrandChoice = null,
                isGooglePayEnabled = false,
                customPaymentMethods = emptyList(),
                elementsSessionId = "session_test",
                passiveCaptcha = null,
                elementsSessionConfigId = null,
                accountId = null,
                merchantId = null,
            )
        }
    }
}

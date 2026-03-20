package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakePaymentMethodFilter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class CreateCustomerStateTest {

    @Test
    fun `Checkout session creates correct state with filtered payment methods`() = runScenario {
        val cards = PaymentMethodFactory.cards(2)
        val customer = CheckoutSessionResponse.Customer(
            id = "cus_checkout_123",
            paymentMethods = cards + listOf(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
            canDetachPaymentMethod = false,
        )
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            customer = customer,
        )
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = "instances_key",
            checkoutSessionResponse = checkoutSessionResponse,
        )

        val result = createCustomerState(
            initializationMode = initializationMode,
            customerMetadata = CustomerMetadata.CheckoutSession(
                sessionId = checkoutSessionResponse.id,
                customerId = "cus_checkout_123",
                removePaymentMethod = PaymentMethodRemovePermission.None,
                saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            ),
        )

        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethods).containsExactlyElementsIn(cards)
        assertThat(result.defaultPaymentMethodId).isNull()
    }

    @Test
    fun `Checkout session with empty payment methods`() = runScenario {
        val customer = CheckoutSessionResponse.Customer(
            id = "cus_checkout_empty",
            paymentMethods = emptyList(),
            canDetachPaymentMethod = false,
        )
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(customer = customer)
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = "instances_key",
            checkoutSessionResponse = checkoutSessionResponse,
        )

        val result = createCustomerState(
            initializationMode = initializationMode,
            customerMetadata = CustomerMetadata.CheckoutSession(
                sessionId = checkoutSessionResponse.id,
                customerId = "cus_checkout_empty",
                removePaymentMethod = PaymentMethodRemovePermission.None,
                saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            ),
        )

        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethods).isEmpty()
        assertThat(result.defaultPaymentMethodId).isNull()
    }

    @Test
    fun `Customer session extracts payment methods and default payment method`() = runScenario {
        val paymentMethods = PaymentMethodFactory.cards(3)
        val defaultPaymentMethodId = paymentMethods[1].id

        val result = createCustomerState(
            customerMetadata = CUSTOMER_SESSION_METADATA,
            elementsSessionCustomer = createElementsSessionCustomer(
                paymentMethods = paymentMethods,
                defaultPaymentMethodId = defaultPaymentMethodId,
            ),
        )

        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethods).isEqualTo(paymentMethods)
        assertThat(result.defaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
    }

    @Test
    fun `Customer session filters payment methods by supported type`() = runScenario {
        val cards = PaymentMethodFactory.cards(2)
        val paymentMethods = cards + listOf(
            PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            PaymentMethodFixtures.AU_BECS_DEBIT,
        )

        val result = createCustomerState(
            customerMetadata = CUSTOMER_SESSION_METADATA,
            elementsSessionCustomer = createElementsSessionCustomer(paymentMethods = paymentMethods),
        )

        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethods).containsExactlyElementsIn(cards)
    }

    @Test
    fun `Customer session with null default payment method`() = runScenario {
        val paymentMethods = PaymentMethodFactory.cards(3)

        val result = createCustomerState(
            customerMetadata = CUSTOMER_SESSION_METADATA,
            elementsSessionCustomer = createElementsSessionCustomer(
                paymentMethods = paymentMethods,
                defaultPaymentMethodId = null,
            ),
        )

        assertThat(result).isNotNull()
        assertThat(result!!.defaultPaymentMethodId).isNull()
    }

    @Test
    fun `Legacy ephemeral key calls repository and sets null default`() = runScenario(
        customerRepository = FakeCustomerRepository(
            paymentMethods = PaymentMethodFactory.cards(3),
        ),
    ) {
        val result = createCustomerState(
            customerMetadata = LEGACY_EK_METADATA,
        )

        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethods).hasSize(3)
        assertThat(result.defaultPaymentMethodId).isNull()

        val request = customerRepository.getPaymentMethodsRequests.awaitItem()
        assertThat(request.customerId).isEqualTo("cus_1")
        assertThat(request.ephemeralKeySecret).isEqualTo("ek_123")
    }

    @Test
    fun `Null customerMetadata returns null`() = runScenario {
        val result = createCustomerState(customerMetadata = null)

        assertThat(result).isNull()
    }

    @Test
    fun `PaymentMethodFilter is applied to result`() = runScenario {
        val cards = PaymentMethodFactory.cards(3)
        val filteredCards = cards.take(1)

        FakePaymentMethodFilter.test(filteredPaymentMethods = filteredCards) {
            val createCustomerState = CreateCustomerState(
                customerRepository = customerRepository,
                paymentMethodFilter = paymentMethodFilter,
            )

            val result = createCustomerState(
                initializationMode = DEFAULT_INITIALIZATION_MODE,
                elementsSession = DEFAULT_ELEMENTS_SESSION.copy(
                    customer = createElementsSessionCustomer(paymentMethods = cards),
                ),
                metadata = PaymentMethodMetadataFactory.create()
                    .copy(customerMetadata = CUSTOMER_SESSION_METADATA),
                savedSelection = CompletableDeferred(SavedSelection.None),
            )

            assertThat(result).isNotNull()
            assertThat(result!!.paymentMethods).isEqualTo(filteredCards)

            val filterCall = filterCalls.awaitItem()
            assertThat(filterCall.paymentMethods).isEqualTo(cards)
        }
    }

    private fun runScenario(
        customerRepository: FakeCustomerRepository = FakeCustomerRepository(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        Scenario(customerRepository = customerRepository).apply {
            block()
            customerRepository.ensureAllEventsConsumed()
        }
    }

    private inner class Scenario(
        val customerRepository: FakeCustomerRepository,
    ) {
        private val createCustomerStateImpl = CreateCustomerState(
            customerRepository = customerRepository,
            paymentMethodFilter = FakePaymentMethodFilter.noOp(),
        )

        suspend fun createCustomerState(
            initializationMode: PaymentElementLoader.InitializationMode = DEFAULT_INITIALIZATION_MODE,
            elementsSession: ElementsSession = DEFAULT_ELEMENTS_SESSION,
            elementsSessionCustomer: ElementsSession.Customer? = elementsSession.customer,
            customerMetadata: CustomerMetadata? = null,
            metadata: com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata =
                PaymentMethodMetadataFactory.create().copy(customerMetadata = customerMetadata),
            savedSelection: CompletableDeferred<SavedSelection> = CompletableDeferred(SavedSelection.None),
        ): CustomerState? {
            val session = if (elementsSessionCustomer != elementsSession.customer) {
                elementsSession.copy(customer = elementsSessionCustomer)
            } else {
                elementsSession
            }

            return createCustomerStateImpl(
                initializationMode = initializationMode,
                elementsSession = session,
                metadata = metadata,
                savedSelection = savedSelection,
            )
        }
    }

    private companion object {
        val DEFAULT_INITIALIZATION_MODE = PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = "pi_123_secret_456",
        )

        val DEFAULT_ELEMENTS_SESSION = ElementsSession(
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
        )

        val CUSTOMER_SESSION_METADATA = CustomerMetadata.CustomerSession(
            id = "cus_1",
            ephemeralKeySecret = "ek_123",
            customerSessionClientSecret = "cuss_1",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
            canRemoveLastPaymentMethod = true,
            canUpdateFullPaymentMethodDetails = true,
        )

        val LEGACY_EK_METADATA = CustomerMetadata.LegacyEphemeralKey(
            id = "cus_1",
            ephemeralKeySecret = "ek_123",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateFullPaymentMethodDetails = false,
        )

        fun createElementsSessionCustomer(
            paymentMethods: List<com.stripe.android.model.PaymentMethod> = PaymentMethodFactory.cards(1),
            defaultPaymentMethodId: String? = null,
        ): ElementsSession.Customer {
            return ElementsSession.Customer(
                paymentMethods = paymentMethods,
                defaultPaymentMethod = defaultPaymentMethodId,
                session = ElementsSession.Customer.Session(
                    id = "cuss_1",
                    customerId = "cus_1",
                    apiKey = "ek_123",
                    apiKeyExpiry = 999999999,
                    liveMode = false,
                    components = ElementsSession.Customer.Components(
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                    ),
                ),
            )
        }
    }
}

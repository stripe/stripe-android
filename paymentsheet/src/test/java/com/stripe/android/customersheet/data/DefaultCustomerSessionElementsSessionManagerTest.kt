package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.utils.FakeCustomerSessionProvider
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCustomerSessionApi::class)
class DefaultCustomerSessionElementsSessionManagerTest {
    @Test
    fun `on fetch elements session, should set parameters properly & report successful load`() = runTest {
        val errorReporter = FakeErrorReporter()
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFactory.create(),
            error = null,
            linkSettings = null,
            sessionsCustomer = createDefaultCustomer(),
        )

        val manager = createElementsSessionManager(
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = errorReporter,
            savedSelection = SavedSelection.PaymentMethod(id = "pm_123"),
            intentConfiguration = Result.success(
                CustomerSheet.IntentConfiguration.Builder()
                    .paymentMethodTypes(paymentMethodTypes = listOf("card", "us_bank_account", "sepa_debit"))
                    .build()
            ),
            customerSessionClientSecret = Result.success(
                CustomerSheet.CustomerSessionClientSecret.create(
                    customerId = "cus_1",
                    clientSecret = "cuss_123",
                )
            ),
        )

        manager.fetchElementsSession()

        val lastParams = elementsSessionRepository.lastParams

        assertThat(lastParams?.defaultPaymentMethodId).isEqualTo("pm_123")
        assertThat(lastParams?.externalPaymentMethods).isEmpty()

        val initializationMode = lastParams?.initializationMode
        assertThat(initializationMode).isInstanceOf(InitializationMode.DeferredIntent::class.java)

        val intentConfiguration = initializationMode.asDeferred().intentConfiguration
        assertThat(intentConfiguration.paymentMethodTypes).containsExactly(
            "card",
            "us_bank_account",
            "sepa_debit"
        )
        assertThat(intentConfiguration.mode).isInstanceOf<PaymentSheet.IntentConfiguration.Mode.Setup>()

        val customer = lastParams?.customer
        assertThat(customer?.id).isEqualTo("cus_1")
        assertThat(customer?.accessType)
            .isEqualTo(PaymentSheet.CustomerAccessType.CustomerSession(customerSessionClientSecret = "cuss_123"))

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.SuccessEvent.CUSTOMER_SHEET_CUSTOMER_SESSION_ELEMENTS_SESSION_LOAD_SUCCESS.eventName,
        )
    }

    @Test
    fun `on fetch elements session, should fail & report if elements session request failed`() = runTest {
        val exception = IllegalStateException("Failed to load!")

        val errorReporter = FakeErrorReporter()

        val elementsSessionManager = createElementsSessionManager(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = SetupIntentFactory.create(),
                error = exception,
                linkSettings = null,
                sessionsCustomer = null,
            ),
            errorReporter = errorReporter,
        )

        val elementsSessionResult = elementsSessionManager.fetchElementsSession()

        assertThat(elementsSessionResult.isFailure).isTrue()
        assertThat(elementsSessionResult.exceptionOrNull()).isEqualTo(exception)

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter
                .ExpectedErrorEvent
                .CUSTOMER_SHEET_CUSTOMER_SESSION_ELEMENTS_SESSION_LOAD_FAILURE
                .eventName,
        )
    }

    @Test
    fun `on fetch elements session, should fail if failed to fetch customer session client secret`() = runTest {
        val exception = IllegalStateException("Failed!")

        val manager = createElementsSessionManager(customerSessionClientSecret = Result.failure(exception))

        val result = manager.fetchElementsSession()

        assertThat(result).isEqualTo(Result.failure<ElementsSession>(exception))
    }

    @Test
    fun `on fetch elements session, should fail if failed to fetch intent configuration`() = runTest {
        val exception = IllegalStateException("Failed!")

        val manager = createElementsSessionManager(intentConfiguration = Result.failure(exception))

        val result = manager.fetchElementsSession()

        assertThat(result).isEqualTo(Result.failure<ElementsSession>(exception))
    }

    @Test
    fun `on multiple elements session calls, should only fetch intent configuration successfully once`() = runTest {
        var amountOfCalls = 0

        val manager = createElementsSessionManager(
            onIntentConfiguration = {
                amountOfCalls++

                Result.success(
                    CustomerSheet.IntentConfiguration.Builder()
                        .paymentMethodTypes(listOf("card", "us_bank_account"))
                        .build()
                )
            }
        )

        manager.fetchElementsSession()
        manager.fetchElementsSession()
        manager.fetchElementsSession()
        manager.fetchElementsSession()
        manager.fetchElementsSession()

        assertThat(amountOfCalls).isEqualTo(1)
    }

    @Test
    fun `on fetch ephemeral key, should fetch from elements session`() = runTest {
        val manager = createElementsSessionManagerWithCustomer(
            apiKey = "ek_123",
            apiKeyExpiry = 999999,
            customerId = "cus_1",
        )

        val result = manager.fetchCustomerSessionEphemeralKey()

        assertThat(result).isEqualTo(
            Result.success(
                CachedCustomerEphemeralKey(
                    customerId = "cus_1",
                    ephemeralKey = "ek_123",
                    expiresAt = 999999,
                    customerSessionClientSecret = "cuss_123",
                )
            )
        )
    }

    @Test
    fun `on fetch ephemeral key, should fail if elements session fetch fails`() = runTest {
        val exception = IllegalStateException("Failed to load!")
        val manager = createElementsSessionManager(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFactory.create(),
                linkSettings = null,
                error = exception,
            )
        )

        val result = manager.fetchCustomerSessionEphemeralKey()

        assertThat(result).isEqualTo(Result.failure<ElementsSession>(exception))
    }

    @Test
    fun `on fetch ephemeral key, should re-use previously fetched key if not expired`() = runTest {
        var amountOfCalls = 0

        val manager = createElementsSessionManagerWithCustomer(
            apiKey = "ek_123",
            apiKeyExpiry = 999999,
            customerId = "cus_1",
            currentTime = 10,
            onCustomerSessionClientSecret = {
                amountOfCalls++

                Result.success(
                    CustomerSheet.CustomerSessionClientSecret.create(
                        customerId = "cus_1",
                        clientSecret = "cuss_123",
                    )
                )
            }
        )

        manager.fetchCustomerSessionEphemeralKey()
        manager.fetchCustomerSessionEphemeralKey()
        manager.fetchCustomerSessionEphemeralKey()

        val lastResult = manager.fetchCustomerSessionEphemeralKey()

        assertThat(amountOfCalls).isEqualTo(1)
        assertThat(lastResult).isEqualTo(
            Result.success(
                CachedCustomerEphemeralKey(
                    customerId = "cus_1",
                    ephemeralKey = "ek_123",
                    expiresAt = 999999,
                    customerSessionClientSecret = "cuss_123",
                )
            )
        )
    }

    @Test
    fun `on fetch ephemeral key, should re-fetch key if expired`() = runTest {
        var amountOfCalls = 0

        val manager = createElementsSessionManagerWithCustomer(
            apiKey = "ek_123",
            apiKeyExpiry = 200000,
            customerId = "cus_1",
            currentTime = 210000000,
            onCustomerSessionClientSecret = {
                amountOfCalls++

                Result.success(
                    CustomerSheet.CustomerSessionClientSecret.create(
                        customerId = "cus_1",
                        clientSecret = "cuss_123",
                    )
                )
            }
        )

        manager.fetchCustomerSessionEphemeralKey()
        manager.fetchCustomerSessionEphemeralKey()
        manager.fetchCustomerSessionEphemeralKey()

        val lastResult = manager.fetchCustomerSessionEphemeralKey()

        assertThat(amountOfCalls).isEqualTo(4)
        assertThat(lastResult).isEqualTo(
            Result.success(
                CachedCustomerEphemeralKey(
                    customerId = "cus_1",
                    ephemeralKey = "ek_123",
                    expiresAt = 200000,
                    customerSessionClientSecret = "cuss_123",
                )
            )
        )
    }

    @Test
    fun `on fetch elements session, should set ephemeral key & be re-used when fetching ephemeral key`() = runTest {
        var amountOfCalls = 0

        val manager = createElementsSessionManagerWithCustomer(
            apiKeyExpiry = 999999,
            currentTime = 10,
            onCustomerSessionClientSecret = {
                amountOfCalls++

                Result.success(
                    CustomerSheet.CustomerSessionClientSecret.create(
                        customerId = "cus_1",
                        clientSecret = "cuss_123",
                    )
                )
            },
        )

        manager.fetchElementsSession()
        val ephemeralKey = manager.fetchCustomerSessionEphemeralKey()

        assertThat(amountOfCalls).isEqualTo(1)

        assertThat(ephemeralKey).isEqualTo(
            Result.success(
                CachedCustomerEphemeralKey(
                    customerId = "cus_1",
                    ephemeralKey = "ek_123",
                    expiresAt = 999999,
                    customerSessionClientSecret = "cuss_123",
                )
            )
        )
    }

    @Test
    fun `on fetch elements session, should fail & report if 'customer' field is empty`() = runTest {
        val errorReporter = FakeErrorReporter()
        val elementsSessionManager = createElementsSessionManager(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = SetupIntentFactory.create(),
                error = null,
                linkSettings = null,
                sessionsCustomer = null,
            ),
            errorReporter = errorReporter,
        )

        val elementsSessionResult = elementsSessionManager.fetchElementsSession()

        assertThat(elementsSessionResult.isFailure).isTrue()
        assertThat(elementsSessionResult.exceptionOrNull()?.message).isEqualTo(
            "`customer` field should be available when using `CustomerSession` in elements/session!"
        )

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.SuccessEvent.CUSTOMER_SHEET_CUSTOMER_SESSION_ELEMENTS_SESSION_LOAD_SUCCESS.eventName,
            ErrorReporter
                .UnexpectedErrorEvent
                .CUSTOMER_SESSION_ON_CUSTOMER_SHEET_ELEMENTS_SESSION_NO_CUSTOMER_FIELD
                .eventName
        )
    }

    @Test
    fun `on fetch elements session, should fail if client secret is empty`() = runClientValidationErrorTest(
        invalidClientSecret = "",
        errorMessage = "The provided 'customerSessionClientSecret' cannot be an empty string."
    )

    @Test
    fun `on fetch elements session, should fail if client secret is legacy ephemeral key`() =
        runClientValidationErrorTest(
            invalidClientSecret = "ek_123",
            errorMessage = "Provided secret looks like an Ephemeral Key secret, but expecting a CustomerSession " +
                "client secret. See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
        )

    @Test
    fun `on fetch elements session, should fail if client secret is not in expected customer session format`() =
        runClientValidationErrorTest(
            invalidClientSecret = "cutt_123",
            errorMessage = "Provided secret does not look like a CustomerSession client secret. " +
                "See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
        )

    private fun runClientValidationErrorTest(
        invalidClientSecret: String,
        errorMessage: String,
    ) = runTest {
        val manager = createElementsSessionManager(
            customerSessionClientSecret = Result.success(
                CustomerSheet.CustomerSessionClientSecret.create(
                    customerId = "cus_1",
                    clientSecret = invalidClientSecret
                )
            )
        )

        val result = manager.fetchElementsSession()

        assertThat(result.isFailure).isTrue()

        val exception = result.exceptionOrNull()

        assertThat(exception?.message).isEqualTo(errorMessage)
    }

    private suspend fun createElementsSessionManagerWithCustomer(
        customerId: String = "cus_1",
        apiKey: String = "ek_123",
        apiKeyExpiry: Int = 999999,
        currentTime: Long = 10,
        onCustomerSessionClientSecret: () -> Result<CustomerSheet.CustomerSessionClientSecret> = {
            Result.success(
                CustomerSheet.CustomerSessionClientSecret.create(
                    customerId = "cus_1",
                    clientSecret = "cuss_123",
                )
            )
        },
    ): CustomerSessionElementsSessionManager {
        val defaultCustomer = createDefaultCustomer()

        return createElementsSessionManager(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFactory.create(),
                error = null,
                linkSettings = null,
                sessionsCustomer = defaultCustomer.copy(
                    session = defaultCustomer.session.copy(
                        apiKey = apiKey,
                        apiKeyExpiry = apiKeyExpiry,
                        customerId = customerId,
                    )
                ),
            ),
            onCustomerSessionClientSecret = onCustomerSessionClientSecret,
            timeProvider = { currentTime }
        )
    }

    private suspend fun createElementsSessionManager(
        elementsSessionRepository: ElementsSessionRepository =
            FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFactory.create(),
                error = null,
                linkSettings = null,
            ),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        intentConfiguration: Result<CustomerSheet.IntentConfiguration> =
            Result.success(CustomerSheet.IntentConfiguration.Builder().build()),
        onIntentConfiguration: () -> Result<CustomerSheet.IntentConfiguration> = { intentConfiguration },
        customerSessionClientSecret: Result<CustomerSheet.CustomerSessionClientSecret> =
            Result.success(
                CustomerSheet.CustomerSessionClientSecret.create(
                    customerId = "cus_1",
                    clientSecret = "cuss_123",
                )
            ),
        onCustomerSessionClientSecret: () -> Result<CustomerSheet.CustomerSessionClientSecret> = {
            customerSessionClientSecret
        },
        savedSelection: SavedSelection? = null,
        timeProvider: () -> Long = {
            10
        }
    ): CustomerSessionElementsSessionManager {
        return DefaultCustomerSessionElementsSessionManager(
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = errorReporter,
            prefsRepositoryFactory = {
                FakePrefsRepository().apply {
                    setSavedSelection(savedSelection = savedSelection)
                }
            },
            customerSessionProvider = FakeCustomerSessionProvider(
                onIntentConfiguration = onIntentConfiguration,
                onProvidesCustomerSessionClientSecret = onCustomerSessionClientSecret,
            ),
            timeProvider = timeProvider,
            workContext = coroutineContext,
        )
    }

    private fun createDefaultCustomer(): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = listOf(),
            defaultPaymentMethod = null,
            session = ElementsSession.Customer.Session(
                id = "cuss_1",
                customerId = "cus_1",
                apiKey = "ek_123",
                apiKeyExpiry = 999999,
                components = ElementsSession.Customer.Components(
                    mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                ),
                liveMode = false,
            )
        )
    }

    private fun InitializationMode?.asDeferred(): InitializationMode.DeferredIntent {
        return this as InitializationMode.DeferredIntent
    }
}

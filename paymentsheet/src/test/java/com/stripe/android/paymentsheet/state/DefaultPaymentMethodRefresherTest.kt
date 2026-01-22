package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import com.stripe.android.utils.FakePaymentMethodFilter
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import kotlin.test.Test

internal class DefaultPaymentMethodRefresherTest {
    @Test
    fun `refresh returns empty list when customerMetadata is null`() = runScenario {
        val metadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = false,
        )

        val result = refresher.refresh(metadata)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `refresh uses elements session for customer sessions path`() {
        val paymentMethods = PaymentMethodFactory.cards(3)
        val defaultPaymentMethodId = paymentMethods[1].id

        runScenario(
            sessionsCustomer = createElementsSessionCustomer(
                paymentMethods = paymentMethods,
                defaultPaymentMethodId = defaultPaymentMethodId,
            ),
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = "cuss_123",
            )

            val result = refresher.refresh(metadata)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEqualTo(paymentMethods)

            val filterCall = filterCalls.awaitItem()
            assertThat(filterCall.paymentMethods).isEqualTo(paymentMethods)
            assertThat(filterCall.params.remoteDefaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
        }
    }

    @Test
    fun `refresh fails when elements session has sessionsError`() {
        val sessionsError = IllegalStateException("Sessions error")

        runScenario(
            sessionsError = sessionsError,
            sessionsCustomer = createElementsSessionCustomer(),
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = "cuss_123",
            )

            val result = refresher.refresh(metadata)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).isEqualTo(sessionsError.message)
        }
    }

    @Test
    fun `refresh fails when elements session repository returns error`() {
        val error = IllegalStateException("Network error")

        runScenario(
            elementsSessionError = error,
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = "cuss_123",
            )

            val result = refresher.refresh(metadata)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).isEqualTo(error.message)
        }
    }

    @Test
    fun `refresh uses customer repository for legacy path`() {
        val paymentMethods = PaymentMethodFactory.cards(2)

        runScenario(
            customerRepoPaymentMethods = paymentMethods,
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = null,
            )

            val result = refresher.refresh(metadata)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEqualTo(paymentMethods)

            val filterCall = filterCalls.awaitItem()
            assertThat(filterCall.paymentMethods).isEqualTo(paymentMethods)
            assertThat(filterCall.params.remoteDefaultPaymentMethodId).isNull()
        }
    }

    @Test
    fun `refresh fails when customer repository returns error`() {
        val error = IllegalStateException("Customer fetch error")

        runScenario(
            customerRepoPaymentMethodsError = error,
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = null,
            )

            val result = refresher.refresh(metadata)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).isEqualTo(error.message)
        }
    }

    @Test
    fun `refresh returns empty list when CustomerSheet integration metadata is used`() {
        runScenario(
            sessionsCustomer = createElementsSessionCustomer(
                paymentMethods = PaymentMethodFactory.cards(2),
            ),
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = "cuss_123",
                integrationMetadata = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_INTEGRATION_METADATA,
            )

            val result = refresher.refresh(metadata)

            assertThat(result.isSuccess).isTrue()

            val filterCall = filterCalls.awaitItem()
            assertThat(filterCall.paymentMethods).isEmpty()
        }
    }

    @Test
    fun `refresh uses local saved selection for elements session request`() {
        val savedPaymentMethodId = "pm_saved_123"
        val prefsRepository = FakePrefsRepository()
        prefsRepository.setSavedSelection(SavedSelection.PaymentMethod(savedPaymentMethodId))

        runScenario(
            sessionsCustomer = createElementsSessionCustomer(),
            prefsRepository = prefsRepository,
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = "cuss_123",
            )

            refresher.refresh(metadata)

            assertThat(filterCalls.awaitItem()).isNotNull()

            assertThat(elementsSessionRepository.lastParams?.savedPaymentMethodSelectionId)
                .isEqualTo(savedPaymentMethodId)
        }
    }

    @Test
    fun `refresh passes country override to elements session`() = runScenario(
        sessionsCustomer = createElementsSessionCustomer(),
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                countryCode = "US",
            ),
            hasCustomerConfiguration = true,
            customerSessionClientSecret = "cuss_123",
            userCountryOverride = "CA",
        )

        refresher.refresh(metadata)

        assertThat(filterCalls.awaitItem()).isNotNull()

        assertThat(elementsSessionRepository.lastParams?.userOverrideCountry).isEqualTo("CA")
    }

    @Test
    fun `refresh passes card filters from metadata to payment method filter`() {
        val paymentMethods = PaymentMethodFactory.cards(5)
        val filteredPaymentMethods = paymentMethods.take(2)

        val cardBrandFilter = FakeCardBrandFilter()
        val cardFundingFilter = FakeCardFundingFilter()

        runScenario(
            customerRepoPaymentMethods = paymentMethods,
            filteredPaymentMethods = filteredPaymentMethods,
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                customerSessionClientSecret = null,
                cardBrandFilter = cardBrandFilter,
                cardFundingFilter = cardFundingFilter,
            )

            val result = refresher.refresh(metadata)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEqualTo(filteredPaymentMethods)

            val filterCall = filterCalls.awaitItem()

            assertThat(filterCall.params.cardBrandFilter).isEqualTo(cardBrandFilter)
            assertThat(filterCall.params.cardFundingFilter).isEqualTo(cardFundingFilter)
        }
    }

    private fun runScenario(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        elementsSessionError: Throwable? = null,
        sessionsError: Throwable? = null,
        sessionsCustomer: ElementsSession.Customer? = null,
        customerRepoPaymentMethods: List<PaymentMethod> = emptyList(),
        customerRepoPaymentMethodsError: Throwable? = null,
        filteredPaymentMethods: List<PaymentMethod>? = null,
        prefsRepository: FakePrefsRepository = FakePrefsRepository(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = stripeIntent,
            error = elementsSessionError,
            sessionsError = sessionsError,
            linkSettings = null,
            sessionsCustomer = sessionsCustomer,
        )

        val customerRepository = if (customerRepoPaymentMethodsError != null) {
            FakeCustomerRepository(onGetPaymentMethods = { Result.failure(customerRepoPaymentMethodsError) })
        } else {
            FakeCustomerRepository(paymentMethods = customerRepoPaymentMethods)
        }

        FakePaymentMethodFilter.test(filteredPaymentMethods = filteredPaymentMethods) {
            val refresher = DefaultPaymentMethodRefresher(
                workContext = backgroundScope.coroutineContext,
                elementsSessionRepository = elementsSessionRepository,
                customerRepository = customerRepository,
                paymentMethodFilter = paymentMethodFilter,
                prefsRepositoryFactory = { prefsRepository },
            )

            Scenario(
                refresher = refresher,
                elementsSessionRepository = elementsSessionRepository,
                customerRepository = customerRepository,
                filterCalls = filterCalls,
                prefsRepository = prefsRepository,
            ).block()
        }
    }

    @Parcelize
    private class FakeCardBrandFilter : CardBrandFilter {
        override fun isAccepted(cardBrand: CardBrand): Boolean {
            return true
        }

        override fun isAccepted(paymentMethod: PaymentMethod): Boolean {
            return true
        }
    }

    @Parcelize
    private class FakeCardFundingFilter : CardFundingFilter {
        override fun allowedFundingTypesDisplayMessage(): Int? {
            return null
        }

        override fun isAccepted(cardFunding: CardFunding): Boolean {
            return true
        }
    }

    private data class Scenario(
        val refresher: DefaultPaymentMethodRefresher,
        val elementsSessionRepository: FakeElementsSessionRepository,
        val customerRepository: FakeCustomerRepository,
        val filterCalls: ReceiveTurbine<FakePaymentMethodFilter.FilterCall>,
        val prefsRepository: FakePrefsRepository,
    )

    private fun createElementsSessionCustomer(
        paymentMethods: List<PaymentMethod> = emptyList(),
        defaultPaymentMethodId: String? = null,
    ): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = paymentMethods,
            defaultPaymentMethod = defaultPaymentMethodId,
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
        )
    }
}

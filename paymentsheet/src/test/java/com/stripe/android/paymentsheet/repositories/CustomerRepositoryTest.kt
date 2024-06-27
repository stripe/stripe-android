package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException
import kotlin.coroutines.coroutineContext

@RunWith(RobolectricTestRunner::class)
internal class CustomerRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeRepository = mock<StripeRepository>()
    private val errorReporter = FakeErrorReporter()

    private val repository = CustomerApiRepository(
        stripeRepository,
        { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_123") },
        Logger.getInstance(false),
        workContext = testDispatcher,
        errorReporter = errorReporter
    )

    @Before
    fun clearErrorReporter() {
        errorReporter.clear()
    }

    @Test
    fun `getPaymentMethods() should create expected ListPaymentMethodsParams`() =
        runTest {
            givenGetPaymentMethodsReturns(
                Result.success(emptyList())
            )

            repository.getPaymentMethods(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card),
                true,
            )

            verify(stripeRepository).getPaymentMethods(
                listPaymentMethodsParams = eq(
                    ListPaymentMethodsParams(
                        customerId = "customer_id",
                        paymentMethodType = PaymentMethod.Type.Card
                    )
                ),
                productUsageTokens = any(),
                requestOptions = any()
            )
        }

    // PayPal isn't supported as a saved payment method due to issues with on-session.
    // See: https://docs.google.com/document/d/1_bCPJXxhV4Kdgy7LX7HPwpZfElN3a2DcYUooiWC9SgM
    @Test
    fun `getPaymentMethods() should filter unsupported payment method types`() =
        runTest {
            givenGetPaymentMethodsReturns(
                Result.success(emptyList())
            )

            repository.getPaymentMethods(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card, PaymentMethod.Type.PayPal),
                true,
            )

            verify(stripeRepository).getPaymentMethods(
                listPaymentMethodsParams = eq(
                    ListPaymentMethodsParams(
                        customerId = "customer_id",
                        paymentMethodType = PaymentMethod.Type.Card
                    )
                ),
                productUsageTokens = any(),
                requestOptions = any()
            )
        }

    @Test
    fun `getPaymentMethods() should filter cards attached to wallets`() =
        runTest {
            givenGetPaymentMethodsReturns(
                Result.success(emptyList())
            )

            val initialCard = PaymentMethodFixtures.CARD_PAYMENT_METHOD

            val mockedReturnPaymentMethods = listOf(
                initialCard.copy("pm_1"),
                initialCard.copy(
                    id = "pm_2",
                    card = initialCard.card?.copy(
                        wallet = Wallet.GooglePayWallet("3000")
                    )
                ),
                initialCard.copy(
                    id = "pm_3",
                    card = initialCard.card?.copy(
                        wallet = Wallet.SamsungPayWallet("4000")
                    )
                ),
                initialCard.copy(
                    id = "pm_4",
                    card = initialCard.card?.copy(
                        wallet = Wallet.ApplePayWallet("5000")
                    )
                ),
                initialCard.copy(
                    id = "pm_5",
                    card = initialCard.card?.copy(
                        wallet = Wallet.ApplePayWallet("6000")
                    )
                )
            )

            stripeRepository.stub {
                onBlocking {
                    getPaymentMethods(
                        listPaymentMethodsParams = eq(
                            ListPaymentMethodsParams(
                                customerId = "customer_id",
                                paymentMethodType = PaymentMethod.Type.Card
                            )
                        ),
                        productUsageTokens = any(),
                        requestOptions = any()
                    )
                }.thenReturn(Result.success(mockedReturnPaymentMethods))
            }

            val result = repository.getPaymentMethods(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card),
                true,
            ).getOrThrow()

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo("pm_1")
        }

    @Test
    fun `getPaymentsMethods() should keep cards from Link wallet and dedupe them`() = runTest {
        givenGetPaymentMethodsReturns(
            Result.success(emptyList())
        )

        val initialCard = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val mockedReturnPaymentMethods = listOf(
            initialCard.copy("pm_1"),
            initialCard.copy(
                id = "pm_2",
                card = initialCard.card?.copy(
                    wallet = Wallet.GooglePayWallet("3000")
                )
            ),
            initialCard.copy(
                id = "pm_3",
                card = initialCard.card?.copy(
                    wallet = Wallet.LinkWallet("4000")
                )
            ),
            initialCard.copy(
                id = "pm_4",
                card = initialCard.card?.copy(
                    wallet = Wallet.LinkWallet("4000")
                )
            ),
        )

        stripeRepository.stub {
            onBlocking {
                getPaymentMethods(
                    listPaymentMethodsParams = eq(
                        ListPaymentMethodsParams(
                            customerId = "customer_id",
                            paymentMethodType = PaymentMethod.Type.Card
                        )
                    ),
                    productUsageTokens = any(),
                    requestOptions = any()
                )
            }.thenReturn(Result.success(mockedReturnPaymentMethods))
        }

        val result = repository.getPaymentMethods(
            CustomerRepository.CustomerInfo(
                "customer_id",
                "ephemeral_key"
            ),
            listOf(PaymentMethod.Type.Card),
            true,
        ).getOrThrow()

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo("pm_3")
        assertThat(result[1].id).isEqualTo("pm_1")
    }

    @Test
    fun `getPaymentMethods() should return empty list on failure when silent failures`() =
        runTest {
            givenGetPaymentMethodsReturns(
                Result.failure(InvalidParameterException("error"))
            )

            val result = repository.getPaymentMethods(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card),
                silentlyFail = true,
            )

            assertThat(result.getOrNull()).isEmpty()
            assertThat(errorReporter.getLoggedErrors())
                .containsExactly(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE.eventName)
        }

    @Test
    fun `getPaymentMethods() should return failure`() =
        runTest {
            givenGetPaymentMethodsReturns(
                Result.failure(InvalidParameterException("error"))
            )

            val result = repository.getPaymentMethods(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card),
                silentlyFail = false,
            )

            assertThat(result.exceptionOrNull()?.message)
                .isEqualTo("error")
            assertThat(errorReporter.getLoggedErrors())
                .containsExactly(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE.eventName)
        }

    @Test
    fun `getPaymentMethods() with partially failing requests should emit list with successful values when silent failures`() =
        runTest {
            val errorReporter = FakeErrorReporter()
            val repository = CustomerApiRepository(
                failsOnceStripeRepository(),
                { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
                Logger.getInstance(false),
                workContext = testDispatcher,
                errorReporter = errorReporter
            )

            // Requesting 3 payment method types, the first request will fail
            val result = repository.getPaymentMethods(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Card, PaymentMethod.Type.Card),
                silentlyFail = true,
            )

            assertThat(result.getOrNull()).containsExactly(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
            assertThat(errorReporter.getLoggedErrors())
                .containsExactly(
                    // Two successes, one failure
                    ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE.eventName,
                    ErrorReporter.SuccessEvent.GET_SAVED_PAYMENT_METHODS_SUCCESS.eventName,
                    ErrorReporter.SuccessEvent.GET_SAVED_PAYMENT_METHODS_SUCCESS.eventName,
                )
        }

    @Test
    fun `getPaymentMethods() with partially failing requests should emit failure`() =
        runTest {
            val errorReporter = FakeErrorReporter()
            val repository = CustomerApiRepository(
                failsOnceStripeRepository(),
                { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
                Logger.getInstance(false),
                workContext = testDispatcher,
                errorReporter = errorReporter
            )

            // Requesting 3 payment method types, the first request will fail
            val result = repository.getPaymentMethods(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Card, PaymentMethod.Type.Card),
                silentlyFail = false,
            )

            assertThat(result.exceptionOrNull()?.message)
                .isEqualTo("Request Failed")
            assertThat(errorReporter.getLoggedErrors())
                .containsExactly(
                    // Two successes, one failure
                    ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE.eventName,
                    ErrorReporter.SuccessEvent.GET_SAVED_PAYMENT_METHODS_SUCCESS.eventName,
                    ErrorReporter.SuccessEvent.GET_SAVED_PAYMENT_METHODS_SUCCESS.eventName,
                )
        }

    @Test
    fun `detachPaymentMethod() should return payment method on success`() =
        runTest {
            givenDetachPaymentMethodReturns(
                Result.success(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )

            val result = repository.detachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                paymentMethodId = "payment_method_id",
                canRemoveDuplicates = false,
            )

            assertThat(result.getOrNull()).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

    @Test
    fun `detachPaymentMethod() should return exception on failure`() =
        runTest {
            givenDetachPaymentMethodReturns(
                Result.failure(InvalidParameterException("error"))
            )

            val result = repository.detachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                paymentMethodId = "payment_method_id",
                canRemoveDuplicates = false,
            )

            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `detachPaymentMethod() with 'canRemoveDuplicates' as 'true' should remove duplicate payment methods and remove provided payment method ID last`() =
        runTest {
            val paymentMethodsToRemove = createCardsWithSameFingerprint(count = 5)
            val removedPaymentMethods = mutableListOf<PaymentMethod>()

            val repository = createCustomerRepository(
                stripeRepository = FakeStripeRepository(
                    onGetPaymentMethods = { _, _, _ -> Result.success(paymentMethodsToRemove) },
                    onDetach = { _, paymentMethodId, _ ->
                        val paymentMethod = paymentMethodsToRemove.first { paymentMethod ->
                            paymentMethod.id == paymentMethodId
                        }

                        removedPaymentMethods.add(paymentMethod)

                        Result.success(paymentMethod)
                    }
                )
            )

            val paymentMethodToRemove = paymentMethodsToRemove.first()

            repository.detachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "customer_id",
                    ephemeralKeySecret = "ephemeral_key"
                ),
                paymentMethodId = paymentMethodToRemove.id!!,
                canRemoveDuplicates = true,
            )

            assertThat(removedPaymentMethods).containsExactlyElementsIn(paymentMethodsToRemove)
            assertThat(removedPaymentMethods.last()).isEqualTo(paymentMethodToRemove)
        }

    @Test
    fun `detachPaymentMethod() with 'canRemoveDuplicates' as 'true' still removes payment method even if not a card`() =
        runTest {
            val usBankAccount = PaymentMethodFactory.usBankAccount()
            val cards = PaymentMethodFactory.cards(2)

            val paymentMethods = listOf(usBankAccount) + cards

            val removedPaymentMethods = mutableListOf<PaymentMethod>()

            val repository = createCustomerRepository(
                stripeRepository = FakeStripeRepository(
                    onGetPaymentMethods = { _, _, _ -> Result.success(cards) },
                    onDetach = { _, paymentMethodId, _ ->
                        val paymentMethod = paymentMethods.first { paymentMethod ->
                            paymentMethod.id == paymentMethodId
                        }

                        removedPaymentMethods.add(paymentMethod)

                        Result.success(paymentMethod)
                    }
                )
            )

            val usBankAccountToRemove = paymentMethods.first()

            repository.detachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "customer_id",
                    ephemeralKeySecret = "ephemeral_key"
                ),
                paymentMethodId = usBankAccountToRemove.id!!,
                canRemoveDuplicates = true,
            )

            assertThat(removedPaymentMethods).containsExactlyElementsIn(listOf(usBankAccount))
        }

    @Test
    fun `detachPaymentMethod() with 'canRemoveDuplicates' as 'false' should not remove duplicate payment methods`() =
        runTest {
            val paymentMethods = createCardsWithSameFingerprint(count = 5)
            val removedPaymentMethods = mutableListOf<PaymentMethod>()

            val repository = createCustomerRepository(
                stripeRepository = FakeStripeRepository(
                    onGetPaymentMethods = { _, _, _ -> Result.success(paymentMethods) },
                    onDetach = { _, paymentMethodId, _ ->
                        val paymentMethod = paymentMethods.first { paymentMethod ->
                            paymentMethod.id == paymentMethodId
                        }

                        removedPaymentMethods.add(paymentMethod)

                        Result.success(paymentMethod)
                    }
                )
            )

            val paymentMethodToRemove = paymentMethods.first()

            repository.detachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "customer_id",
                    ephemeralKeySecret = "ephemeral_key"
                ),
                paymentMethodId = paymentMethodToRemove.id!!,
                canRemoveDuplicates = false,
            )

            val duplicates = paymentMethods.filter { paymentMethod ->
                paymentMethod.id != paymentMethodToRemove.id
            }

            assertThat(removedPaymentMethods).containsNoneIn(duplicates)
        }

    @Test
    fun `detachPaymentMethod() with removing duplicates enabled should return failure if duplicate failure occurs`() =
        runTest {
            val paymentMethods = createCardsWithSameFingerprint(count = 5)
            val removedPaymentMethods = mutableListOf<PaymentMethod>()

            val failToRemovePaymentMethods = paymentMethods.subList(3, 5)

            val repository = createCustomerRepository(
                stripeRepository = FakeStripeRepository(
                    onGetPaymentMethods = { _, _, _ -> Result.success(paymentMethods) },
                    onDetach = { _, paymentMethodId, _ ->
                        if (failToRemovePaymentMethods.any { it.id == paymentMethodId }) {
                            return@FakeStripeRepository Result.failure<PaymentMethod>(
                                exception = IllegalStateException("Failed to remove!")
                            )
                        }

                        val paymentMethod = paymentMethods.first { paymentMethod ->
                            paymentMethod.id == paymentMethodId
                        }

                        removedPaymentMethods.add(paymentMethod)

                        Result.success(paymentMethod)
                    }
                )
            )

            val result = repository.detachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "customer_id",
                    ephemeralKeySecret = "ephemeral_key"
                ),
                paymentMethodId = paymentMethods.first().id!!,
                canRemoveDuplicates = true,
            )

            assertThat(result.isFailure)
                .isTrue()

            val exception = result.exceptionOrNull().asDuplicateFailureException()

            val hasFailedPaymentMethodIds = exception.failures.all { failure ->
                failToRemovePaymentMethods.any { paymentMethod ->
                    paymentMethod.id == failure.paymentMethodId
                }
            }

            assertThat(hasFailedPaymentMethodIds).isTrue()
        }

    @Test
    fun `attachPaymentMethod() should return payment method on success`() =
        runTest {
            givenAttachPaymentMethodReturns(
                Result.success(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )

            val result = repository.attachPaymentMethod(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            assertThat(result).isEqualTo(
                Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        }

    @Test
    fun `attachPaymentMethod() should return failure`() =
        runTest {
            val error = Result.failure<PaymentMethod>(InvalidParameterException("error"))
            givenAttachPaymentMethodReturns(error)

            val result = repository.attachPaymentMethod(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            assertThat(result).isEqualTo(error)
        }

    @Test
    fun `updatePaymentMethod() should return payment method on success`() =
        runTest {
            val success = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            givenUpdatePaymentMethodReturns(success)

            val result = repository.updatePaymentMethod(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                paymentMethodId = "payment_method_id",
                params = PaymentMethodUpdateParams.createCard()
            )

            assertThat(result).isEqualTo(success)
        }

    @Test
    fun `updatePaymentMethod() should return failure`() =
        runTest {
            val error = Result.failure<PaymentMethod>(InvalidParameterException("error"))
            givenUpdatePaymentMethodReturns(error)

            val result = repository.updatePaymentMethod(
                CustomerRepository.CustomerInfo(
                    "customer_id",
                    "ephemeral_key"
                ),
                paymentMethodId = "payment_method_id",
                params = PaymentMethodUpdateParams.createCard()
            )

            assertThat(result).isEqualTo(error)
        }

    private suspend fun createCustomerRepository(
        stripeRepository: StripeRepository,
    ): CustomerApiRepository {
        return CustomerApiRepository(
            workContext = coroutineContext,
            errorReporter = errorReporter,
            stripeRepository = stripeRepository,
            lazyPaymentConfig = { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_123") },
            logger = Logger.getInstance(false),
        )
    }

    private suspend fun failsOnceStripeRepository(): StripeRepository {
        val repository = mock<StripeRepository>()
        whenever(
            repository.getPaymentMethods(
                listPaymentMethodsParams = any(),
                productUsageTokens = any(),
                requestOptions = any()
            )
        )
            .doReturn(Result.failure(InvalidParameterException("Request Failed")))
            .doReturn(Result.success(listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)))
        return repository
    }

    private fun givenGetPaymentMethodsReturns(
        result: Result<List<PaymentMethod>>
    ) {
        stripeRepository.stub {
            onBlocking {
                getPaymentMethods(
                    listPaymentMethodsParams = any(),
                    productUsageTokens = any(),
                    requestOptions = any(),
                )
            }.doReturn(result)
        }
    }

    private fun givenDetachPaymentMethodReturns(
        result: Result<PaymentMethod>
    ) {
        stripeRepository.stub {
            onBlocking {
                detachPaymentMethod(
                    productUsageTokens = any(),
                    paymentMethodId = anyString(),
                    requestOptions = any(),
                )
            }.doReturn(result)
        }
    }

    private fun givenAttachPaymentMethodReturns(
        result: Result<PaymentMethod>
    ) {
        stripeRepository.stub {
            onBlocking {
                attachPaymentMethod(
                    customerId = anyString(),
                    productUsageTokens = any(),
                    paymentMethodId = anyString(),
                    requestOptions = any(),
                )
            }.doReturn(result)
        }
    }

    private fun givenUpdatePaymentMethodReturns(
        result: Result<PaymentMethod>
    ) {
        stripeRepository.stub {
            onBlocking {
                updatePaymentMethod(
                    paymentMethodId = any(),
                    paymentMethodUpdateParams = any(),
                    options = any(),
                )
            }.doReturn(result)
        }
    }

    private fun Throwable?.asDuplicateFailureException(): DuplicatePaymentMethodDetachFailureException {
        return this as DuplicatePaymentMethodDetachFailureException
    }

    private fun createCardsWithSameFingerprint(count: Int): List<PaymentMethod> {
        return PaymentMethodFactory.cards(count).map { paymentMethod ->
            paymentMethod.copy(
                card = paymentMethod.card?.copy(
                    fingerprint = "1234567890"
                )
            )
        }
    }

    private class FakeStripeRepository(
        val onGetPaymentMethods: suspend (
            listPaymentMethodsParams: ListPaymentMethodsParams,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ) -> Result<List<PaymentMethod>>,
        val onDetach: suspend (
            productUsageTokens: Set<String>,
            paymentMethodId: String,
            requestOptions: ApiRequest.Options
        ) -> Result<PaymentMethod>
    ) : AbsFakeStripeRepository() {
        override suspend fun detachPaymentMethod(
            productUsageTokens: Set<String>,
            paymentMethodId: String,
            requestOptions: ApiRequest.Options
        ): Result<PaymentMethod> {
            return onDetach(
                productUsageTokens,
                paymentMethodId,
                requestOptions,
            )
        }

        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): Result<List<PaymentMethod>> {
            return onGetPaymentMethods(
                listPaymentMethodsParams,
                productUsageTokens,
                requestOptions,
            )
        }
    }
}

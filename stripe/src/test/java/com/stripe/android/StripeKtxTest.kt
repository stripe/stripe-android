package com.stripe.android

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.StripeModel
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.AfterTest
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Test for [Stripe] suspend functions.
 */
@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
internal class StripeKtxTest {
    private val mockApiRepository: StripeApiRepository = mock()
    private val mockPaymentController: PaymentController = mock()

    private val testDispatcher = TestCoroutineDispatcher()

    private val stripe: Stripe =
        Stripe(
            mockApiRepository,
            mockPaymentController,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            TEST_STRIPE_ACCOUNT_ID,
            TestCoroutineDispatcher()
        )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `When repository returns correct value then createPaymentMethod should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI then returns correct result`(
            mockApiRepository::createPaymentMethod,
            stripe::createPaymentMethod
        )

    @Test
    fun `When repository throws exception then createPaymentMethod should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI then throws same exception`(
            mockApiRepository::createPaymentMethod,
            stripe::createPaymentMethod
        )

    @Test
    fun `When repository returns null then createPaymentMethod should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI then thorws APIException`(
            mockApiRepository::createPaymentMethod,
            stripe::createPaymentMethod
        )

    @Test
    fun `When repository returns correct value then createSource should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI then returns correct result`(
            mockApiRepository::createSource,
            stripe::createSource
        )

    @Test
    fun `When repository throws exception then createSource should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI then throws same exception`(
            mockApiRepository::createSource,
            stripe::createSource
        )

    @Test
    fun `When repository returns null then createSource should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI then thorws APIException`(
            mockApiRepository::createSource,
            stripe::createSource
        )

    @Test
    fun `When repository returns correct value then createAccountToken should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI then returns correct result`(
            mockApiRepository::createToken,
            stripe::createAccountToken
        )

    @Test
    fun `When repository throws exception then createAccountToken should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI then throws same exception`(
            mockApiRepository::createToken,
            stripe::createAccountToken
        )

    @Test
    fun `When repository returns null then createAccountToken should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI then thorws APIException`(
            mockApiRepository::createToken,
            stripe::createAccountToken
        )

    @Test
    fun `When repository returns correct value then createBankAccountToken should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI then returns correct result`(
            mockApiRepository::createToken,
            stripe::createBankAccountToken
        )

    @Test
    fun `When repository throws exception then createBankAccountToken should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI then throws same exception`(
            mockApiRepository::createToken,
            stripe::createBankAccountToken
        )

    @Test
    fun `When repository returns null then createBankAccountToken should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI then thorws APIException`(
            mockApiRepository::createToken,
            stripe::createBankAccountToken
        )

    @Test
    fun `When repository returns correct value then createPiiToken should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI with String param then returns correct result`(
            mockApiRepository::createToken,
            stripe::createPiiToken
        )

    @Test
    fun `When repository throws exception then createPiiToken should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI with String param then throws same exception`(
            mockApiRepository::createToken,
            stripe::createPiiToken
        )

    @Test
    fun `When repository returns null then createPiiToken should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI with String param then throws APIException`(
            mockApiRepository::createToken,
            stripe::createPiiToken
        )

    @Test
    fun `When repository returns correct value then createCardToken should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI then returns correct result`(
            mockApiRepository::createToken,
            stripe::createCardToken
        )

    @Test
    fun `When repository throws exception then createCardToken should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI then throws same exception`(
            mockApiRepository::createToken,
            stripe::createCardToken
        )

    @Test
    fun `When repository returns null then createCardToken should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI then thorws APIException`(
            mockApiRepository::createToken,
            stripe::createCardToken
        )

    @Test
    fun `When repository returns correct value then createCvcUpdateToken should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI with String param then returns correct result`(
            mockApiRepository::createToken,
            stripe::createCvcUpdateToken
        )

    @Test
    fun `When repository throws exception then createCvcUpdateToken should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI with String param then throws same exception`(
            mockApiRepository::createToken,
            stripe::createCvcUpdateToken
        )

    @Test
    fun `When repository returns null then createCvcUpdateToken should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI with String param then throws APIException`(
            mockApiRepository::createToken,
            stripe::createCvcUpdateToken
        )

    @Test
    fun `When repository returns correct value then createPersonToken should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI then returns correct result`(
            mockApiRepository::createToken,
            stripe::createPersonToken
        )

    @Test
    fun `When repository throws exception then createPersonToken should throw same exception`(): Unit =
        `Given repository throws exception when calling createAPI then throws same exception`(
            mockApiRepository::createToken,
            stripe::createPersonToken
        )

    @Test
    fun `When repository returns null then createPersonToken should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI then thorws APIException`(
            mockApiRepository::createToken,
            stripe::createPersonToken
        )

    @Test
    fun `When repository returns correct value then createFile should Succeed`(): Unit =
        `Given repository returns non-empty value when calling createAPI then returns correct result`(
            mockApiRepository::createFile,
            stripe::createFile
        )

    @Test
    fun `When repository returns null then createFile should throw APIException`(): Unit =
        `Given repository returns empty value when calling createAPI then thorws APIException`(
            mockApiRepository::createFile,
            stripe::createFile
        )

    @Test
    fun `When repository returns correct value then retrievePaymentIntent should Succeed`(): Unit =
        `Given repository returns non-empty value when calling retrieveAPI with String param then returns correct result`(
            mockApiRepository::retrievePaymentIntent,
            stripe::retrievePaymentIntent
        )

    @Test
    fun `When repository throws exception then retrievePaymentIntent should throw same exception`(): Unit =
        `Given repository throws exception when calling retrieveAPI with String param then throws same exception`(
            mockApiRepository::retrievePaymentIntent,
            stripe::retrievePaymentIntent
        )

    @Test
    fun `When repository returns null then retrievePaymentIntent should throw APIException`(): Unit =
        `Given repository returns empty value when calling retrieveAPI with String param then throws APIException`(
            mockApiRepository::retrievePaymentIntent,
            stripe::retrievePaymentIntent
        )

    @Test
    fun `When repository returns correct value then retrieveSetupIntent should Succeed`(): Unit =
        `Given repository returns non-empty value when calling retrieveAPI with String param then returns correct result`(
            mockApiRepository::retrieveSetupIntent,
            stripe::retrieveSetupIntent
        )

    @Test
    fun `When repository throws exception then retrieveSetupIntent should throw same exception`(): Unit =
        `Given repository throws exception when calling retrieveAPI with String param then throws same exception`(
            mockApiRepository::retrieveSetupIntent,
            stripe::retrieveSetupIntent
        )

    @Test
    fun `When repository returns null then retrieveSetupIntent should throw APIException`(): Unit =
        `Given repository returns empty value when calling retrieveAPI with String param then throws APIException`(
            mockApiRepository::retrieveSetupIntent,
            stripe::retrieveSetupIntent
        )

    @Test
    fun `When repository returns correct value then retrieveSource should Succeed`() = testDispatcher.runBlockingTest {
        val expectedApiObj = mock<Source>()
        whenever(
            mockApiRepository.retrieveSource(any(), any(), any())
        ).thenReturn(expectedApiObj)
        val actualObj = stripe.retrieveSource(
            "param11",
            "param12",
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    @Test
    fun `When repository throws exception then retrieveSource should throw same exception`(): Unit =
        testDispatcher.runBlockingTest {
            whenever(
                mockApiRepository.retrieveSource(any(), any(), any())
            ).thenThrow(mock<AuthenticationException>())

            assertFailsWith<AuthenticationException> {
                stripe.retrieveSource(
                    "param11",
                    "param12",
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When repository returns null then retrieveSource should throw APIException`(): Unit =
        testDispatcher.runBlockingTest {
            whenever(
                mockApiRepository.retrieveSource(any(), any(), any())
            ).thenReturn(null)

            assertFailsWith<InvalidRequestException> {
                stripe.retrieveSource(
                    "param11",
                    "param12",
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When repository returns correct value then confirmSetupIntentSuspend should Succeed`() =
        testDispatcher.runBlockingTest {
            val expectedApiObj = mock<SetupIntent>()
            whenever(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenReturn(expectedApiObj)
            val actualObj = stripe.confirmSetupIntent(mock())

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When repository throws exception then confirmSetupIntentSuspend should throw same exception`(): Unit =
        testDispatcher.runBlockingTest {
            whenever(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenThrow(mock<AuthenticationException>())

            assertFailsWith<AuthenticationException> {
                stripe.confirmSetupIntent(mock())
            }
        }

    @Test
    fun `When repository returns null then confirmSetupIntentSuspend should throw APIException`(): Unit =
        testDispatcher.runBlockingTest {
            whenever(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenReturn(null)

            assertFailsWith<InvalidRequestException> {
                stripe.confirmSetupIntent(mock())
            }
        }

    @Test
    fun `When repository returns correct value then confirmPaymentIntentSuspend should Succeed`() =
        testDispatcher.runBlockingTest {
            val expectedApiObj = mock<PaymentIntent>()
            whenever(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenReturn(expectedApiObj)
            val actualObj = stripe.confirmPaymentIntent(mock())

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When repository throws exception then confirmPaymentIntentSuspend should throw same exception`(): Unit =
        testDispatcher.runBlockingTest {
            whenever(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenThrow(mock<AuthenticationException>())

            assertFailsWith<AuthenticationException> {
                stripe.confirmPaymentIntent(mock())
            }
        }

    @Test
    fun `When repository returns null then confirmPaymentIntentSuspend should throw APIException`(): Unit =
        testDispatcher.runBlockingTest {
            whenever(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenReturn(null)

            assertFailsWith<InvalidRequestException> {
                stripe.confirmPaymentIntent(mock())
            }
        }

    private inline fun <reified APIObject : StripeModel, reified CreateAPIParam : Any, reified RepositoryParam : Any>
    `Given repository returns non-empty value when calling createAPI then returns correct result`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (CreateAPIParam, String?, String?) -> APIObject
    ) = testDispatcher.runBlockingTest {
        val expectedApiObj = mock<APIObject>()

        whenever(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(expectedApiObj)

        val actualObj = createAPIInvocationBlock(
            mock(),
            TEST_IDEMPOTENCY_KEY,
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <reified CreateAPIParam : Any, reified RepositoryParam : Any>
    `Given repository throws exception when calling createAPI then throws same exception`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> StripeModel?,
        crossinline createAPIInvocationBlock: suspend (CreateAPIParam, String?, String?) -> StripeModel
    ): Unit = testDispatcher.runBlockingTest {
        whenever(
            repositoryInvocationBlock(any(), any())
        ).thenThrow(mock<AuthenticationException>())

        assertFailsWith<AuthenticationException> {
            createAPIInvocationBlock(
                mock(),
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified CreateAPIParam : Any, reified RepositoryParam : Any>
    `Given repository returns empty value when calling createAPI then thorws APIException`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> StripeModel?,
        crossinline createAPIInvocationBlock: suspend (CreateAPIParam, String?, String?) -> StripeModel
    ): Unit = testDispatcher.runBlockingTest {
        whenever(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(null)

        assertFailsWith<InvalidRequestException> {
            createAPIInvocationBlock(
                mock(),
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified APIObject : StripeModel, reified RepositoryParam : Any>
    `Given repository returns non-empty value when calling createAPI with String param then returns correct result`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (String, String?, String?) -> APIObject
    ) = testDispatcher.runBlockingTest {
        val expectedApiObj = mock<APIObject>()

        whenever(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(expectedApiObj)

        val actualObj = createAPIInvocationBlock(
            "param1",
            TEST_IDEMPOTENCY_KEY,
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <reified RepositoryParam : Any>
    `Given repository throws exception when calling createAPI with String param then throws same exception`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> StripeModel?,
        crossinline createAPIInvocationBlock: suspend (String, String?, String?) -> StripeModel
    ): Unit = testDispatcher.runBlockingTest {
        whenever(
            repositoryInvocationBlock(any(), any())
        ).thenThrow(mock<AuthenticationException>())

        assertFailsWith<AuthenticationException> {
            createAPIInvocationBlock(
                "param1",
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified RepositoryParam : Any>
    `Given repository returns empty value when calling createAPI with String param then throws APIException`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> StripeModel?,
        crossinline createAPIInvocationBlock: suspend (String, String?, String?) -> StripeModel
    ): Unit = testDispatcher.runBlockingTest {
        whenever(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(null)

        assertFailsWith<InvalidRequestException> {
            createAPIInvocationBlock(
                "param1",
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified APIObject : StripeModel>
    `Given repository returns non-empty value when calling retrieveAPI with String param then returns correct result`(
        crossinline repositoryInvocationBlock: suspend (String, ApiRequest.Options, List<String>) -> APIObject?,
        crossinline retrieveAPIInvocationBlock: suspend (String, String?) -> APIObject
    ): Unit = testDispatcher.runBlockingTest {
        val expectedApiObj = mock<APIObject>()

        whenever(
            repositoryInvocationBlock(any(), any(), any())
        ).thenReturn(expectedApiObj)

        val actualObj = retrieveAPIInvocationBlock(
            "param1",
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private fun
    `Given repository throws exception when calling retrieveAPI with String param then throws same exception`(
        repositoryInvocationBlock: suspend (String, ApiRequest.Options, List<String>) -> StripeModel?,
        retrieveAPIInvocationBlock: suspend (String, String?) -> StripeModel
    ): Unit = testDispatcher.runBlockingTest {
        whenever(
            repositoryInvocationBlock(any(), any(), any())
        ).thenThrow(mock<AuthenticationException>())

        assertFailsWith<AuthenticationException> {
            retrieveAPIInvocationBlock(
                "param1",
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private fun
    `Given repository returns empty value when calling retrieveAPI with String param then throws APIException`(
        repositoryInvocationBlock: suspend (String, ApiRequest.Options, List<String>) -> StripeModel?,
        retrieveAPIInvocationBlock: suspend (String, String?) -> StripeModel
    ): Unit = testDispatcher.runBlockingTest {
        whenever(
            repositoryInvocationBlock(any(), any(), any())
        ).thenReturn(null)

        assertFailsWith<InvalidRequestException> {
            retrieveAPIInvocationBlock(
                "param1",
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private companion object {
        const val TEST_IDEMPOTENCY_KEY = "test_idempotenc_key"
        const val TEST_STRIPE_ACCOUNT_ID = "test_account_id"
    }
}

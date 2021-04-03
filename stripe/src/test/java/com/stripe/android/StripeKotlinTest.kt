package com.stripe.android

import android.content.Intent
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Test for [Stripe] suspend functions.
 */
@RunWith(JUnit4::class)
internal class StripeKotlinTest {
    private val mockApiRepository: StripeApiRepository = mock()
    private val mockPaymentController: PaymentController = mock()

    private val stripe: Stripe =
        Stripe(mockApiRepository, mockPaymentController, TEST_PUBLISHABLE_KEY)

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
    fun `When repository returns correct value then retrieveSource should Succeed`() = runBlocking {
        val expectedApiObj = mock<Source>()
        `when`(
            mockApiRepository.retrieveSource(any(), any(), any())
        ).thenReturn(expectedApiObj)
        val actualObj = stripe.retrieveSource(
            "Dummy String param1",
            "Dummy String param2",
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    @Test
    fun `When repository throws exception then retrieveSource should throw same exception`(): Unit =
        runBlocking {
            `when`(
                mockApiRepository.retrieveSource(any(), any(), any())
            ).thenThrow(mock<AuthenticationException>())

            assertFailsWith<AuthenticationException> {
                stripe.retrieveSource(
                    "Dummy String param1",
                    "Dummy String param2",
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When repository returns null then retrieveSource should throw APIException`(): Unit =
        runBlocking {
            `when`(
                mockApiRepository.retrieveSource(any(), any(), any())
            ).thenReturn(null)

            assertFailsWith<APIException> {
                stripe.retrieveSource(
                    "Dummy String param1",
                    "Dummy String param2",
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When repository returns correct value then confirmSetupIntentSuspend should Succeed`() =
        runBlocking {
            val expectedApiObj = mock<SetupIntent>()
            `when`(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenReturn(expectedApiObj)
            val actualObj = stripe.confirmSetupIntentSuspend(mock())

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When repository throws exception then confirmSetupIntentSuspend should throw same exception`(): Unit =
        runBlocking {
            `when`(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenThrow(mock<AuthenticationException>())

            assertFailsWith<AuthenticationException> {
                stripe.confirmSetupIntentSuspend(mock())
            }
        }

    @Test
    fun `When repository returns null then confirmSetupIntentSuspend should throw APIException`(): Unit =
        runBlocking {
            `when`(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenReturn(null)

            assertFailsWith<APIException> {
                stripe.confirmSetupIntentSuspend(mock())
            }
        }

    @Test
    fun `When repository returns correct value then confirmPaymentIntentSuspend should Succeed`() =
        runBlocking {
            val expectedApiObj = mock<PaymentIntent>()
            `when`(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenReturn(expectedApiObj)
            val actualObj = stripe.confirmPaymentIntentSuspend(mock())

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When repository throws exception then confirmPaymentIntentSuspend should throw same exception`(): Unit =
        runBlocking {
            `when`(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenThrow(mock<AuthenticationException>())

            assertFailsWith<AuthenticationException> {
                stripe.confirmPaymentIntentSuspend(mock())
            }
        }

    @Test
    fun `When repository returns null then confirmPaymentIntentSuspend should throw APIException`(): Unit =
        runBlocking {
            `when`(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenReturn(null)

            assertFailsWith<APIException> {
                stripe.confirmPaymentIntentSuspend(mock())
            }
        }

    @Test
    fun `When controller returns correct value then getPaymentIntentResult should succeed`(): Unit =
        `Given controller returns non-empty value when calling getAPI then returns correct result`(
            mockPaymentController::shouldHandlePaymentResult,
            mockPaymentController::getPaymentIntentResult,
            stripe::getPaymentIntentResult
        )

    @Test
    fun `When isNotForSetupIntent then getPaymentIntentResult should throw IllegalArgumentException`(): Unit =
        `Given controller check fails when calling getAPI then throws IllegalArgumentException`(
            mockPaymentController::shouldHandlePaymentResult,
            stripe::getPaymentIntentResult
        )

    @Test
    fun `When controller throws exception then getPaymentIntentResult should throw same exception`(): Unit =
        `Given controller returns exception when calling getAPI then throws same exception`(
            mockPaymentController::shouldHandlePaymentResult,
            mockPaymentController::getPaymentIntentResult,
            stripe::getPaymentIntentResult
        )

    @Test
    fun `When controller returns correct value then getSetupIntentResult should succeed`(): Unit =
        `Given controller returns non-empty value when calling getAPI then returns correct result`(
            mockPaymentController::shouldHandleSetupResult,
            mockPaymentController::getSetupIntentResult,
            stripe::getSetupIntentResult
        )

    @Test
    fun `When isNotForSetupIntent then getSetupIntentResult should throw IllegalArgumentException`(): Unit =
        `Given controller check fails when calling getAPI then throws IllegalArgumentException`(
            mockPaymentController::shouldHandleSetupResult,
            stripe::getSetupIntentResult
        )

    @Test
    fun `When controller throws exception then getSetupIntentResult should throw same exception`(): Unit =
        `Given controller returns exception when calling getAPI then throws same exception`(
            mockPaymentController::shouldHandleSetupResult,
            mockPaymentController::getSetupIntentResult,
            stripe::getSetupIntentResult
        )

    @Test
    fun `When controller returns correct value then getAuthenticateSourceResult should succeed`(): Unit =
        `Given controller returns non-empty value when calling getAPI then returns correct result`(
            mockPaymentController::shouldHandleSourceResult,
            mockPaymentController::getSource,
            stripe::getAuthenticateSourceResult
        )

    @Test
    fun `When isNotForSetupIntent then getAuthenticateSourceResult should throw IllegalArgumentException`(): Unit =
        `Given controller check fails when calling getAPI then throws IllegalArgumentException`(
            mockPaymentController::shouldHandleSourceResult,
            stripe::getAuthenticateSourceResult
        )

    @Test
    fun `When controller throws exception then getAuthenticateSourceResult should throw same exception`(): Unit =
        `Given controller returns exception when calling getAPI then throws same exception`(
            mockPaymentController::shouldHandleSourceResult,
            mockPaymentController::getSource,
            stripe::getAuthenticateSourceResult
        )

    private inline fun <reified APIObject : Any, reified CreateAPIParam : Any, reified RepositoryParam : Any>
    `Given repository returns non-empty value when calling createAPI then returns correct result`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (CreateAPIParam, String?, String?) -> APIObject
    ) = runBlocking {
        val expectedApiObj = mock<APIObject>()

        `when`(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(expectedApiObj)

        val actualObj = createAPIInvocationBlock(
            mock(),
            TEST_IDEMPOTENCY_KEY,
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <APIObject : Any, reified CreateAPIParam : Any, reified RepositoryParam : Any>
    `Given repository throws exception when calling createAPI then throws same exception`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (CreateAPIParam, String?, String?) -> APIObject
    ): Unit = runBlocking {
        `when`(
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

    private inline fun <APIObject : Any, reified CreateAPIParam : Any, reified RepositoryParam : Any>
    `Given repository returns empty value when calling createAPI then thorws APIException`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (CreateAPIParam, String?, String?) -> APIObject
    ): Unit = runBlocking {
        `when`(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(null)

        assertFailsWith<APIException> {
            createAPIInvocationBlock(
                mock(),
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified APIObject : Any, reified RepositoryParam : Any>
    `Given repository returns non-empty value when calling createAPI with String param then returns correct result`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (String, String?, String?) -> APIObject
    ) = runBlocking {
        val expectedApiObj = mock<APIObject>()

        `when`(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(expectedApiObj)

        val actualObj = createAPIInvocationBlock(
            "Dummy String param",
            TEST_IDEMPOTENCY_KEY,
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <APIObject : Any, reified RepositoryParam : Any>
    `Given repository throws exception when calling createAPI with String param then throws same exception`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (String, String?, String?) -> APIObject
    ): Unit = runBlocking {
        `when`(
            repositoryInvocationBlock(any(), any())
        ).thenThrow(mock<AuthenticationException>())

        assertFailsWith<AuthenticationException> {
            createAPIInvocationBlock(
                "Dummy String param",
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <APIObject : Any, reified RepositoryParam : Any>
    `Given repository returns empty value when calling createAPI with String param then throws APIException`(
        crossinline repositoryInvocationBlock: suspend (RepositoryParam, ApiRequest.Options) -> APIObject?,
        crossinline createAPIInvocationBlock: suspend (String, String?, String?) -> APIObject
    ): Unit = runBlocking {
        `when`(
            repositoryInvocationBlock(any(), any())
        ).thenReturn(null)

        assertFailsWith<APIException> {
            createAPIInvocationBlock(
                "Dummy String param",
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified APIObject : Any>
    `Given repository returns non-empty value when calling retrieveAPI with String param then returns correct result`(
        crossinline repositoryInvocationBlock: suspend (String, ApiRequest.Options, List<String>) -> APIObject?,
        crossinline retrieveAPIInvocationBlock: suspend (String, String?) -> APIObject
    ) = runBlocking {
        val expectedApiObj = mock<APIObject>()

        `when`(
            repositoryInvocationBlock(any(), any(), any())
        ).thenReturn(expectedApiObj)

        val actualObj = retrieveAPIInvocationBlock(
            "Dummy String param",
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <APIObject : Any>
    `Given repository throws exception when calling retrieveAPI with String param then throws same exception`(
        crossinline repositoryInvocationBlock: suspend (String, ApiRequest.Options, List<String>) -> APIObject?,
        crossinline retrieveAPIInvocationBlock: suspend (String, String?) -> APIObject
    ): Unit = runBlocking {
        `when`(
            repositoryInvocationBlock(any(), any(), any())
        ).thenThrow(mock<AuthenticationException>())

        assertFailsWith<AuthenticationException> {
            retrieveAPIInvocationBlock(
                "Dummy String param",
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <APIObject : Any>
    `Given repository returns empty value when calling retrieveAPI with String param then throws APIException`(
        crossinline repositoryInvocationBlock: suspend (String, ApiRequest.Options, List<String>) -> APIObject?,
        crossinline retrieveAPIInvocationBlock: suspend (String, String?) -> APIObject
    ): Unit = runBlocking {
        `when`(
            repositoryInvocationBlock(any(), any(), any())
        ).thenReturn(null)

        assertFailsWith<APIException> {
            retrieveAPIInvocationBlock(
                "Dummy String param",
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified APIObject : Any>
    `Given controller returns non-empty value when calling getAPI then returns correct result`(
        crossinline controllerCheckBlock: (Int, Intent?) -> Boolean,
        crossinline controllerInvocationBlock: suspend (Intent) -> APIObject,
        crossinline getAPIInvocationBlock: suspend (Int, Intent?) -> APIObject
    ) = runBlocking {
        val expectedApiObj = mock<APIObject>()

        `when`(
            controllerCheckBlock(any(), any())
        ).thenReturn(true)

        `when`(
            controllerInvocationBlock(any())
        ).thenReturn(expectedApiObj)

        val actualObj = getAPIInvocationBlock(
            TEST_REQUEST_CODE,
            mock()
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <APIObject : Any>
    `Given controller check fails when calling getAPI then throws IllegalArgumentException`(
        crossinline controllerCheckBlock: (Int, Intent?) -> Boolean,
        crossinline getAPIInvocationBlock: suspend (Int, Intent?) -> APIObject
    ): Unit = runBlocking {
        `when`(
            controllerCheckBlock(any(), any())
        ).thenReturn(false)

        assertFailsWith<IllegalArgumentException> {
            getAPIInvocationBlock(
                TEST_REQUEST_CODE,
                mock()
            )
        }
    }

    private inline fun <APIObject : Any>
    `Given controller returns exception when calling getAPI then throws same exception`(
        crossinline controllerCheckBlock: (Int, Intent?) -> Boolean,
        crossinline controllerInvocationBlock: suspend (Intent) -> APIObject,
        crossinline getAPIInvocationBlock: suspend (Int, Intent?) -> APIObject
    ): Unit = runBlocking {
        `when`(
            controllerCheckBlock(any(), any())
        ).thenReturn(true)

        `when`(
            controllerInvocationBlock(any())
        ).thenThrow(mock<AuthenticationException>())

        assertFailsWith<AuthenticationException> {
            getAPIInvocationBlock(
                TEST_REQUEST_CODE,
                mock()
            )
        }
    }

    private companion object {
        const val TEST_PUBLISHABLE_KEY = "test_publishable_key"
        const val TEST_IDEMPOTENCY_KEY = "test_idempotenc_key"
        const val TEST_STRIPE_ACCOUNT_ID = "test_account_id"
        const val TEST_REQUEST_CODE = 1
    }
}

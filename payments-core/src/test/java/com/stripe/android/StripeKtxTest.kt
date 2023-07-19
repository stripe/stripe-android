package com.stripe.android

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.StripeParamsModel
import com.stripe.android.model.WeChatPayNextAction
import com.stripe.android.networking.StripeApiRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Test for [Stripe] suspend functions.
 */
@RunWith(JUnit4::class)
internal class StripeKtxTest {
    private val mockApiRepository: StripeApiRepository = mock()
    private val mockPaymentController: PaymentController = mock()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val stripe: Stripe =
        Stripe(
            mockApiRepository,
            mockPaymentController,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            TEST_STRIPE_ACCOUNT_ID,
            testDispatcher
        )

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
    fun `When repository returns correct value then createFile should Succeed`(): Unit =
        runTest {
            val expectedFile = mock<StripeFile>()

            whenever(
                mockApiRepository.createFile(any(), any())
            ).thenReturn(Result.success(expectedFile))

            val actualFile = stripe.createFile(
                mock(),
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )

            assertSame(expectedFile, actualFile)
        }

    @Test
    fun `When repository returns failure then createFile should throw InvalidRequestException`(): Unit =
        runTest {
            whenever(
                mockApiRepository.createFile(any(), any())
            ).thenReturn(Result.failure(IllegalArgumentException("Failed to parse StripeFile.")))

            assertFailsWithMessage<InvalidRequestException>("Failed to parse StripeFile.") {
                stripe.createFile(
                    mock(),
                    TEST_IDEMPOTENCY_KEY,
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When repository returns correct value then retrievePaymentIntent should Succeed`(): Unit =
        `Given repository returns non-empty value when calling retrieveAPI with String param then return correct result`(
            mockApiRepository::retrievePaymentIntent,
            stripe::retrievePaymentIntent
        )

    @Test
    fun `When repository throws exception then retrievePaymentIntent should throw same exception`(): Unit =
        `Given repository throws exception when calling retrieveAPI with String param then returns a failure`(
            mockApiRepository::retrievePaymentIntent,
            stripe::retrievePaymentIntent
        )

    @Test
    fun `When repository returns correct value then retrieveSetupIntent should Succeed`(): Unit =
        `Given repository returns non-empty value when calling retrieveAPI with String param then return correct result`(
            mockApiRepository::retrieveSetupIntent,
            stripe::retrieveSetupIntent
        )

    @Test
    fun `When repository throws exception then retrieveSetupIntent should throw same exception`(): Unit =
        `Given repository throws exception when calling retrieveAPI with String param then returns a failure`(
            mockApiRepository::retrieveSetupIntent,
            stripe::retrieveSetupIntent
        )

    @Test
    fun `When repository returns correct value then retrieveSource should Succeed`() = runTest {
        val expectedApiObj = mock<Source>()
        whenever(
            mockApiRepository.retrieveSource(any(), any(), any())
        ).thenReturn(Result.success(expectedApiObj))
        val actualObj = stripe.retrieveSource(
            "param11",
            "param12",
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    @Test
    fun `When repository throws exception then retrieveSource should throw same exception`(): Unit =
        runTest {
            whenever(
                mockApiRepository.retrieveSource(any(), any(), any())
            ).thenReturn(Result.failure(mock<AuthenticationException>()))

            assertFailsWith<AuthenticationException> {
                stripe.retrieveSource(
                    "param11",
                    "param12",
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When repository returns correct value then confirmSetupIntentSuspend should Succeed`() =
        runTest {
            val expectedApiObj = mock<SetupIntent>()
            whenever(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenReturn(Result.success(expectedApiObj))
            val actualObj = stripe.confirmSetupIntent(mock())

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When repository throws exception then confirmSetupIntentSuspend should throw same exception`(): Unit =
        runTest {
            whenever(
                mockApiRepository.confirmSetupIntent(any(), any(), any())
            ).thenReturn(Result.failure(mock<AuthenticationException>()))

            assertFailsWith<AuthenticationException> {
                stripe.confirmSetupIntent(mock())
            }
        }

    @Test
    fun `When repository returns correct value then confirmPaymentIntentSuspend should Succeed`() =
        runTest {
            val expectedApiObj = mock<PaymentIntent>()
            whenever(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenReturn(Result.success(expectedApiObj))
            val actualObj = stripe.confirmPaymentIntent(mock())

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When repository throws exception then confirmPaymentIntentSuspend should throw same exception`(): Unit =
        runTest {
            whenever(
                mockApiRepository.confirmPaymentIntent(any(), any(), any())
            ).thenReturn(Result.failure(mock<AuthenticationException>()))

            assertFailsWith<AuthenticationException> {
                stripe.confirmPaymentIntent(mock())
            }
        }

    @Test
    fun `When controller throws exception then confirmAlipayPayment should throw same exception`(): Unit =
        runTest {
            whenever(
                mockPaymentController.confirmAndAuthenticateAlipay(any(), any(), any())
            ).thenReturn(Result.failure(mock<AuthenticationException>()))

            assertFailsWith<AuthenticationException> {
                stripe.confirmAlipayPayment(
                    mock(),
                    mock(),
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When controller returns correct value then confirmAlipayPayment should succeed`(): Unit =
        runTest {
            val expectedApiObj = mock<PaymentIntentResult>()

            whenever(
                mockPaymentController.confirmAndAuthenticateAlipay(any(), any(), any())
            ).thenReturn(Result.success(expectedApiObj))

            val actualObj = stripe.confirmAlipayPayment(
                mock(),
                mock(),
                TEST_STRIPE_ACCOUNT_ID
            )

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When controller returns correct value then getPaymentIntentResult should succeed`(): Unit =
        `Given controller returns non-empty value when calling getAPI then returns correct result`(
            mockPaymentController::shouldHandlePaymentResult,
            mockPaymentController::getPaymentIntentResult,
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
    fun `When isNotForSetupIntent then getPaymentIntentResult should throw InvalidRequestException`(): Unit =
        `Given controller check fails when calling getAPI then throws InvalidRequestException`(
            mockPaymentController::shouldHandlePaymentResult,
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
    fun `When controller throws exception then getSetupIntentResult should throw same exception`(): Unit =
        `Given controller returns exception when calling getAPI then throws same exception`(
            mockPaymentController::shouldHandleSetupResult,
            mockPaymentController::getSetupIntentResult,
            stripe::getSetupIntentResult
        )

    @Test
    fun `When isNotForSetupIntent then getSetupIntentResult should throw InvalidRequestException`(): Unit =
        `Given controller check fails when calling getAPI then throws InvalidRequestException`(
            mockPaymentController::shouldHandleSetupResult,
            stripe::getSetupIntentResult
        )

    @Test
    fun `When controller returns correct value then getAuthenticateSourceResult should succeed`(): Unit =
        `Given controller returns non-empty value when calling getAPI then returns correct result`(
            mockPaymentController::shouldHandleSourceResult,
            mockPaymentController::getAuthenticateSourceResult,
            stripe::getAuthenticateSourceResult
        )

    @Test
    fun `When controller throws exception then getAuthenticateSourceResult should throw same exception`(): Unit =
        `Given controller returns exception when calling getAPI then throws same exception`(
            mockPaymentController::shouldHandleSourceResult,
            mockPaymentController::getAuthenticateSourceResult,
            stripe::getAuthenticateSourceResult
        )

    @Test
    fun `When isNotForSetupIntent then getAuthenticateSourceResult should throw InvalidRequestException`(): Unit =
        `Given controller check fails when calling getAPI then throws InvalidRequestException`(
            mockPaymentController::shouldHandleSourceResult,
            stripe::getAuthenticateSourceResult
        )

    @Test
    fun `When controller returns correct value then confirmWeChatPayPayment should succeed`(): Unit =
        runTest {
            val expectedApiObj = mock<WeChatPayNextAction>()

            whenever(
                mockPaymentController.confirmWeChatPay(any(), any())
            ).thenReturn(Result.success(expectedApiObj))

            val actualObj = stripe.confirmWeChatPayPayment(
                mock(),
                TEST_STRIPE_ACCOUNT_ID
            )

            assertSame(expectedApiObj, actualObj)
        }

    @Test
    fun `When controller throws exception then confirmWeChatPayPayment should throw same exception`(): Unit =
        runTest {
            whenever(
                mockPaymentController.confirmWeChatPay(any(), any())
            ).thenReturn(Result.failure(mock<AuthenticationException>()))

            assertFailsWith<AuthenticationException> {
                stripe.confirmWeChatPayPayment(
                    mock(),
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `When nextAction is not for WeChatPay then should throw InvalidRequestException`(): Unit =
        // when nextAction is not for WeChatPay, mockPaymentController fails in `require` and
        // throws an IllegalArgumentException
        runTest {
            whenever(
                mockPaymentController.confirmWeChatPay(any(), any())
            ).thenReturn(Result.failure(mock<IllegalArgumentException>()))

            assertFailsWith<InvalidRequestException> {
                stripe.confirmWeChatPayPayment(
                    mock(),
                    TEST_STRIPE_ACCOUNT_ID
                )
            }
        }

    @Test
    fun `Verify retrievePaymentIntent passes expand fields on to repository`(): Unit = runTest {
        whenever(
            mockApiRepository.retrievePaymentIntent(
                clientSecret = isA(),
                options = isA(),
                expandFields = isA(),
            )
        ).doReturn(Result.success(mock()))

        val expandFields = listOf("payment_method")

        stripe.retrievePaymentIntent(
            clientSecret = "clientSecret",
            expand = expandFields,
        )

        verify(mockApiRepository).retrievePaymentIntent(
            clientSecret = eq("clientSecret"),
            options = isA(),
            expandFields = eq(expandFields),
        )
    }

    @Test
    fun `Verify retrieveSetupIntent passes expand fields on to repository`(): Unit = runTest {
        whenever(
            mockApiRepository.retrieveSetupIntent(isA(), isA(), isA())
        ).doReturn(Result.success(mock()))

        val expandFields = listOf("payment_method")

        stripe.retrieveSetupIntent(
            clientSecret = "clientSecret",
            expand = expandFields,
        )

        verify(mockApiRepository).retrieveSetupIntent(
            clientSecret = isA(),
            options = isA(),
            expandFields = eq(expandFields),
        )
    }

    @Test
    fun `Verify confirmSetupIntent passes expand fields on to repository`(): Unit = runTest {
        whenever(mockApiRepository.confirmSetupIntent(isA(), isA(), isA())).thenReturn(
            Result.success(mock())
        )

        val expandFields = listOf("payment_method")

        stripe.confirmSetupIntent(
            confirmSetupIntentParams = mock(),
            expand = expandFields,
        )

        verify(mockApiRepository).confirmSetupIntent(
            confirmSetupIntentParams = isA(),
            options = isA(),
            expandFields = eq(expandFields),
        )
    }

    @Test
    fun `Verify retrievePaymentIntent with callback passes expand fields on to repository`() = runTest {
        val expandFields = listOf("payment_method")

        stripe.retrievePaymentIntent(
            clientSecret = "pi_123_secret_123",
            expand = expandFields,
            callback = mock(),
        )

        verify(mockApiRepository).retrievePaymentIntent(
            clientSecret = eq("pi_123_secret_123"),
            options = isA(),
            expandFields = eq(expandFields),
        )
    }

    @Test
    fun `Verify retrievePaymentIntentSynchronous passes expand fields on to repository`() = runTest {
        val expandFields = listOf("payment_method")

        stripe.retrievePaymentIntentSynchronous(
            clientSecret = "pi_123_secret_123",
            expand = expandFields,
        )

        verify(mockApiRepository).retrievePaymentIntent(
            clientSecret = eq("pi_123_secret_123"),
            options = isA(),
            expandFields = eq(expandFields),
        )
    }

    @Test
    fun `Verify retrieveSetupIntent with callback passes expand fields on to repository`() = runTest {
        val expandFields = listOf("payment_method")

        stripe.retrieveSetupIntent(
            clientSecret = "seti_123_secret_123",
            expand = expandFields,
            callback = mock(),
        )

        verify(mockApiRepository).retrieveSetupIntent(
            clientSecret = eq("seti_123_secret_123"),
            options = isA(),
            expandFields = eq(expandFields),
        )
    }

    @Test
    fun `Verify retrieveSetupIntentSynchronous passes expand fields on to repository`() = runTest {
        val expandFields = listOf("payment_method")

        stripe.retrieveSetupIntentSynchronous(
            clientSecret = "seti_123_secret_123",
            expand = expandFields,
        )

        verify(mockApiRepository).retrieveSetupIntent(
            clientSecret = eq("seti_123_secret_123"),
            options = isA(),
            expandFields = eq(expandFields),
        )
    }

    @Test
    fun `Verify retrievePossibleCardBrands passes card number on to repository`() = runTest {
        whenever(mockApiRepository.retrieveCardMetadata(any(), any())).thenReturn(
            Result.success(
                CardMetadata(
                    bin = mock(),
                    accountRanges = listOf()
                )
            )
        )

        stripe.retrievePossibleBrands("4242 42")

        verify(mockApiRepository).retrieveCardMetadata(
            cardNumber = eq("4242 42"),
            requestOptions = isA()
        )
    }

    @Test
    fun `Verify retrievePossibleCardBrands throws an error if repository returns failure`() = runTest {
        whenever(mockApiRepository.retrieveCardMetadata(any(), any())).thenReturn(
            Result.failure(InvalidRequestException(message = "cardNumber cannot be less than 6 characters"))
        )

        val error = assertFailsWith<InvalidRequestException> {
            stripe.retrievePossibleBrands("4242")
        }

        assertThat(error.message).isEqualTo("cardNumber cannot be less than 6 characters")
    }

    private inline fun <reified ApiObject : StripeModel, reified CreateAPIParam : StripeParamsModel, reified RepositoryParam : StripeParamsModel>
    `Given repository returns non-empty value when calling createAPI then returns correct result`(
        crossinline repositoryBlock: suspend (RepositoryParam, ApiRequest.Options) -> Result<ApiObject>,
        crossinline createApiInvocationBlock: suspend (CreateAPIParam, String?, String?) -> ApiObject
    ) = runTest {
        val expectedApiObj = mock<ApiObject>()

        whenever(
            repositoryBlock(any(), any())
        ).thenReturn(Result.success(expectedApiObj))

        val actualObj = createApiInvocationBlock(
            mock(),
            TEST_IDEMPOTENCY_KEY,
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <reified CreateAPIParam : StripeParamsModel, reified RepositoryParam : StripeParamsModel>
    `Given repository throws exception when calling createAPI then throws same exception`(
        crossinline repositoryBlock: suspend (RepositoryParam, ApiRequest.Options) -> Result<StripeModel>,
        crossinline createApiInvocationBlock: suspend (CreateAPIParam, String?, String?) -> StripeModel
    ): Unit = runTest {
        whenever(
            repositoryBlock(any(), any())
        ).thenReturn(Result.failure(mock<AuthenticationException>()))

        assertFailsWith<AuthenticationException> {
            createApiInvocationBlock(
                mock(),
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified ApiObject : StripeModel, reified RepositoryParam : StripeParamsModel>
    `Given repository returns non-empty value when calling createAPI with String param then returns correct result`(
        crossinline repositoryBlock: suspend (RepositoryParam, ApiRequest.Options) -> Result<ApiObject>,
        crossinline createApiInvocationBlock: suspend (String, String?, String?) -> ApiObject
    ) = runTest {
        val expectedApiObj = mock<ApiObject>()

        whenever(
            repositoryBlock(any(), any())
        ).thenReturn(Result.success(expectedApiObj))

        val actualObj = createApiInvocationBlock(
            "param1",
            TEST_IDEMPOTENCY_KEY,
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <reified RepositoryParam : StripeParamsModel>
    `Given repository throws exception when calling createAPI with String param then throws same exception`(
        crossinline repositoryBlock: suspend (RepositoryParam, ApiRequest.Options) -> Result<StripeModel>,
        crossinline createApiInvocationBlock: suspend (String, String?, String?) -> StripeModel
    ): Unit = runTest {
        whenever(
            repositoryBlock(any(), any())
        ).thenReturn(Result.failure(mock<AuthenticationException>()))

        assertFailsWith<AuthenticationException> {
            createApiInvocationBlock(
                "param1",
                TEST_IDEMPOTENCY_KEY,
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified ApiObject : StripeModel>
    `Given repository returns non-empty value when calling retrieveAPI with String param then return correct result`(
        crossinline repositoryBlock: suspend (String, ApiRequest.Options, List<String>) -> Result<ApiObject>,
        crossinline retrieveApiInvocationBlock: suspend (String, String?) -> ApiObject
    ): Unit = runTest {
        val expectedApiObj = mock<ApiObject>()

        whenever(
            repositoryBlock(any(), any(), any())
        ).thenReturn(Result.success(expectedApiObj))

        val actualObj = retrieveApiInvocationBlock(
            "param1",
            TEST_STRIPE_ACCOUNT_ID
        )

        assertSame(expectedApiObj, actualObj)
    }

    private fun
    `Given repository throws exception when calling retrieveAPI with String param then returns a failure`(
        repositoryBlock: suspend (String, ApiRequest.Options, List<String>) -> Result<StripeModel>,
        retrieveApiInvocationBlock: suspend (String, String?) -> StripeModel
    ): Unit = runTest {
        whenever(
            repositoryBlock(any(), any(), any())
        ).thenReturn(Result.failure(mock<AuthenticationException>()))

        assertFailsWith<AuthenticationException> {
            retrieveApiInvocationBlock(
                "param1",
                TEST_STRIPE_ACCOUNT_ID
            )
        }
    }

    private inline fun <reified ApiObject : StripeModel>
    `Given controller returns non-empty value when calling getAPI then returns correct result`(
        crossinline controllerCheckBlock: (Int, Intent?) -> Boolean,
        crossinline controllerInvocationBlock: suspend (Intent) -> Result<ApiObject>,
        crossinline getAPIInvocationBlock: suspend (Int, Intent) -> ApiObject
    ) = runTest {
        val expectedApiObj = mock<ApiObject>()

        whenever(
            controllerCheckBlock(any(), any())
        ).thenReturn(true)

        whenever(
            controllerInvocationBlock(any())
        ).thenReturn(Result.success(expectedApiObj))

        val actualObj = getAPIInvocationBlock(
            TEST_REQUEST_CODE,
            mock()
        )

        assertSame(expectedApiObj, actualObj)
    }

    private inline fun <ApiObject : StripeModel>
    `Given controller returns exception when calling getAPI then throws same exception`(
        crossinline controllerCheckBlock: (Int, Intent?) -> Boolean,
        crossinline controllerInvocationBlock: suspend (Intent) -> Result<ApiObject>,
        crossinline getAPIInvocationBlock: suspend (Int, Intent) -> ApiObject
    ): Unit = runTest {
        whenever(
            controllerCheckBlock(any(), any())
        ).thenReturn(true)

        whenever(
            controllerInvocationBlock(any())
        ).thenReturn(Result.failure(mock<AuthenticationException>()))

        assertFailsWith<AuthenticationException> {
            getAPIInvocationBlock(
                TEST_REQUEST_CODE,
                mock()
            )
        }
    }

    private inline fun <reified ApiObject : StripeModel>
    `Given controller check fails when calling getAPI then throws InvalidRequestException`(
        crossinline controllerCheckBlock: (Int, Intent?) -> Boolean,
        crossinline getAPIInvocationBlock: suspend (Int, Intent) -> ApiObject
    ): Unit = runTest {
        whenever(
            controllerCheckBlock(any(), any())
        ).thenReturn(false)

        assertFailsWith<InvalidRequestException>("Incorrect requestCode and data for ${ApiObject::class.java.simpleName}.") {
            getAPIInvocationBlock(
                TEST_REQUEST_CODE,
                mock()
            )
        }
    }

    private inline fun <reified T : Throwable> assertFailsWithMessage(
        throwableMsg: String,
        block: () -> Unit
    ) {
        val throwable = assertFailsWith<T> { block() }
        assertThat(throwable.message).isEqualTo(throwableMsg)
    }

    private companion object {
        const val TEST_REQUEST_CODE = 1
        const val TEST_IDEMPOTENCY_KEY = "test_idempotenc_key"
        const val TEST_STRIPE_ACCOUNT_ID = "test_account_id"
    }
}

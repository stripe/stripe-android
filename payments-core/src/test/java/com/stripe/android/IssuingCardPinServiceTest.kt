package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.testharness.TestEphemeralKeyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test class for [IssuingCardPinService].
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class IssuingCardPinServiceTest {
    private val retrievalListener: IssuingCardPinService.IssuingCardPinRetrievalListener = mock()
    private val updateListener: IssuingCardPinService.IssuingCardPinUpdateListener = mock()

    private val testDispatcher = TestCoroutineDispatcher()

    private val stripeRepository = FakeStripeRepository()
    private val service = IssuingCardPinService(
        TestEphemeralKeyProvider().also {
            it.setNextRawEphemeralKey(EPHEMERAL_KEY.toString())
        },
        stripeRepository,
        OperationIdFactory.get(),
        "acct_123",
        testDispatcher
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `retrievePin() should call onIssuingCardPinRetrieved() on listener when successful`() {
        stripeRepository.retrievedPin = { PIN }

        service.retrievePin(
            "ic_abcdef",
            "iv_abcd",
            "123-456",
            retrievalListener
        )

        verify(retrievalListener)
            .onIssuingCardPinRetrieved(PIN)
    }

    @Test
    fun `updatePin() should call onIssuingCardPinUpdated() on listener when successful`() {
        service.updatePin(
            "ic_abcdef",
            "5678",
            "iv_abcd",
            "123-456",
            updateListener
        )

        verify(updateListener)
            .onIssuingCardPinUpdated()

        assertThat(stripeRepository.updatePinCalls)
            .isEqualTo(1)
    }

    @Test
    fun `retrievePin() should call onError() on listener when there is an error`() {
        stripeRepository.retrievedPin = {
            throw InvalidRequestException(
                stripeError = StripeError(
                    code = "incorrect_code",
                    message = "Verification failed",
                    type = "invalid_request_error"
                )
            )
        }

        service.retrievePin(
            "ic_abcdef",
            "iv_abcd",
            "123-456",
            retrievalListener
        )

        verify(retrievalListener).onError(
            IssuingCardPinService.CardPinActionError.ONE_TIME_CODE_INCORRECT,
            "The one-time code was incorrect.",
            null
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        var retrievedPin: () -> String? = { null }
        var updatePinCalls = 0

        override suspend fun retrieveIssuingCardPin(
            cardId: String,
            verificationId: String,
            userOneTimeCode: String,
            requestOptions: ApiRequest.Options
        ): String? = retrievedPin()

        override suspend fun updateIssuingCardPin(
            cardId: String,
            newPin: String,
            verificationId: String,
            userOneTimeCode: String,
            requestOptions: ApiRequest.Options
        ) {
            updatePinCalls++
        }
    }

    private companion object {
        private const val PIN = "1234"

        private val EPHEMERAL_KEY = JSONObject(
            """
            {
                "id": "ephkey_123",
                "object": "ephemeral_key",
                "secret": "ek_test_123",
                "created": 1501179335,
                "livemode": false,
                "expires": 1501199335,
                "associated_objects": [{
                    "type": "issuing.card",
                    "id": "ic_abcd"
                }]
            }
            """.trimIndent()
        )
    }
}

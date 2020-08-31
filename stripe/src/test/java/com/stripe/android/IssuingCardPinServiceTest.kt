package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.testharness.TestEphemeralKeyProvider
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/**
 * Test class for [IssuingCardPinService].
 */
@RunWith(RobolectricTestRunner::class)
class IssuingCardPinServiceTest {
    private val stripeApiRequestExecutor: ApiRequestExecutor = mock()
    private val retrievalListener: IssuingCardPinService.IssuingCardPinRetrievalListener = mock()
    private val updateListener: IssuingCardPinService.IssuingCardPinUpdateListener = mock()

    private val stripeRepository = StripeApiRepository(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        stripeApiRequestExecutor = stripeApiRequestExecutor,
        analyticsRequestExecutor = {}
    )
    private val service = IssuingCardPinService(
        TestEphemeralKeyProvider().also {
            it.setNextRawEphemeralKey(EPHEMERAL_KEY.toString())
        },
        stripeRepository,
        OperationIdFactory.get()
    )

    @Test
    fun testRetrieval() {
        val response = StripeResponse(
            200,
            """
            {
                "card": "ic_abcdef",
                "pin": "1234"
            }
            """.trimIndent()
        )

        whenever(
            stripeApiRequestExecutor.execute(
                argThat(
                    ApiRequestMatcher(
                        StripeRequest.Method.GET,
                        "https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin?verification%5Bone_time_code%5D=123-456&verification%5Bid%5D=iv_abcd",
                        ApiRequest.Options("ek_test_123")
                    )
                )
            )
        ).thenReturn(response)

        service.retrievePin(
            "ic_abcdef",
            "iv_abcd",
            "123-456",
            retrievalListener
        )

        verify(retrievalListener)
            .onIssuingCardPinRetrieved("1234")
    }

    @Test
    fun testUpdate() {
        val response = StripeResponse(
            200,
            """
            {
                "card": "ic_abcdef",
                "pin": ""
            }
            """.trimIndent()
        )

        whenever(
            stripeApiRequestExecutor.execute(
                argThat(
                    ApiRequestMatcher(
                        StripeRequest.Method.POST,
                        "https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin",
                        ApiRequest.Options("ek_test_123")
                    )
                )
            )
        ).thenReturn(response)

        service.updatePin(
            "ic_abcdef",
            "1234",
            "iv_abcd",
            "123-456",
            updateListener
        )

        verify(updateListener)
            .onIssuingCardPinUpdated()
    }

    @Test
    fun testRetrievalFailsWithReason() {
        val response = StripeResponse(
            400,
            """
            {
                "error": {
                    "code": "incorrect_code",
                    "message": "Verification failed",
                    "type": "invalid_request_error"
                }
            }
            """.trimIndent()
        )

        whenever(
            stripeApiRequestExecutor.execute(
                argThat(
                    ApiRequestMatcher(
                        StripeRequest.Method.GET,
                        "https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin?verification%5Bone_time_code%5D=123-456&verification%5Bid%5D=iv_abcd",
                        ApiRequest.Options("ek_test_123")
                    )
                )
            )
        ).thenReturn(response)

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

    private companion object {

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

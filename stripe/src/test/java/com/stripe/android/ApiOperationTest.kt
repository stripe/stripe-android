package com.stripe.android

import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.RateLimitException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import java.lang.RuntimeException
import kotlin.test.Test
import kotlinx.coroutines.MainScope
import org.json.JSONException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiOperationTest {

    private val callback: ApiResultCallback<PaymentIntent> = mock()

    @Test
    fun getResult_whenSuccess_shouldInvokeCallbackOnSuccess() {
        FakeApiOperation(
            {
                PaymentIntentFixtures.PI_REQUIRES_3DS1
            },
            callback
        ).execute()
        verify(callback)
            .onSuccess(PaymentIntentFixtures.PI_REQUIRES_3DS1)
    }

    @Test
    fun getResult_whenNull_shouldInvokeCallbackOnError() {
        FakeApiOperation(
            {
                null
            },
            callback
        ).execute()
        verify(callback).onError(
            argWhere {
                (it as? RuntimeException)?.message == "The API operation returned neither a result or exception"
            }
        )
    }

    @Test
    fun getResult_whenJsonException_shouldInvokeCallbackOnError() {
        FakeApiOperation(
            {
                throw JSONException("")
            },
            callback
        ).execute()
        verify(callback).onError(isA<APIException>())
    }

    @Test
    fun getResult_whenRateLimitException_shouldInvokeCallbackOnError() {
        FakeApiOperation(
            {
                throw RateLimitException()
            },
            callback
        ).execute()
        verify(callback).onError(isA<RateLimitException>())
    }

    @Test
    fun getResult_whenIllegalArgumentException_shouldInvokeCallbackOnError() {
        FakeApiOperation(
            {
                throw IllegalArgumentException("Illegal argument!")
            },
            callback
        ).execute()
        verify(callback).onError(
            argWhere {
                (it as? InvalidRequestException)?.message == "Illegal argument!"
            }
        )
    }

    internal class FakeApiOperation(
        private val resultSupplier: () -> PaymentIntent?,
        callback: ApiResultCallback<PaymentIntent>
    ) : ApiOperation<PaymentIntent>(
        workScope = MainScope(),
        callback = callback
    ) {
        override suspend fun getResult(): PaymentIntent? = resultSupplier()
    }
}

package com.stripe.android

import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.exception.InvalidRequestException
import kotlin.test.Test
import kotlinx.coroutines.MainScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiOperationTest {

    private val callback: ApiResultCallback<String> = mock()

    @Test
    fun getResult_whenException_shouldInvokeCallbackOnError() {
        FakeApiOperation(callback).execute()
        verify(callback).onError(
            argWhere {
                (it as? InvalidRequestException)?.message == "Illegal argument!"
            }
        )
    }

    internal class FakeApiOperation(
        callback: ApiResultCallback<String>
    ) : ApiOperation<String>(
        workScope = MainScope(),
        callback = callback
    ) {
        override suspend fun getResult(): String? {
            throw IllegalArgumentException("Illegal argument!")
        }
    }
}

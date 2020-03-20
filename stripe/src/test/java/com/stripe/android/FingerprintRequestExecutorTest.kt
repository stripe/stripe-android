package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.MainScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestExecutorTest {
    private val fingerprintRequestFactory = FingerprintRequestFactory(
        ApplicationProvider.getApplicationContext<Context>()
    )

    @Test
    fun execute_whenSuccessful_shouldReturnResponseWithUuid() {
        create().execute(
            request = fingerprintRequestFactory.create()
        ) { (responseBody) ->
            val uuid = UUID.fromString(responseBody)
            assertThat(uuid.toString())
                .isEqualTo(responseBody)
        }
    }

    @Test
    fun execute_whenConnectionException_shouldNotInvokeCallback() {
        var callbackCount = 0

        val request = fingerprintRequestFactory.create()
        val connectionFactory: ConnectionFactory = mock()
        whenever(connectionFactory.create(request)).thenThrow(IOException())
        create(connectionFactory = connectionFactory)
            .execute(
                request = request,
                callback = {
                    callbackCount++
                }
            )

        assertThat(callbackCount)
            .isEqualTo(0)
    }

    private fun create(
        connectionFactory: ConnectionFactory = ConnectionFactory.Default()
    ) = FingerprintRequestExecutor.Default(
        workScope = MainScope(),
        connectionFactory = connectionFactory
    )
}

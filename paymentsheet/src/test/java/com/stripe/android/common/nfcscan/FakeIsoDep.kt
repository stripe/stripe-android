package com.stripe.android.common.nfcscan

import android.nfc.Tag
import android.nfc.tech.IsoDep
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class FakeIsoDep(
    val tag: Tag = mock(),
) {
    val connectCalls = Turbine<Unit>()
    val transceiveCalls = Turbine<ByteArray>()
    val closeCalls = Turbine<Unit>()

    private var transceiveResults: List<ByteArray> = emptyList()
    private var transceiveResultIndex = 0

    val wrappedInstance: IsoDep = mock()

    init {
        whenever(wrappedInstance.transceive(any())).thenAnswer { invocation ->
            val command = invocation.arguments[0] as ByteArray
            transceiveCalls.add(command)
            if (transceiveResultIndex < transceiveResults.size) {
                transceiveResults[transceiveResultIndex++]
            } else {
                byteArrayOf(0x69.toByte(), 0x82.toByte())
            }
        }

        doAnswer {
            connectCalls.add(Unit)
            null
        }.whenever(wrappedInstance).connect()

        doAnswer {
            closeCalls.add(Unit)
            null
        }.whenever(wrappedInstance).close()
    }

    fun setTransceiveResponses(
        transceiveResults: List<ByteArray>,
    ) {
        this.transceiveResults = transceiveResults
        this.transceiveResultIndex = 0
    }

    suspend fun assertConnect() {
        assertThat(connectCalls.awaitItem()).isEqualTo(Unit)
    }

    suspend fun assertCommand(command: ByteArray) {
        assertThat(transceiveCalls.awaitItem()).isEqualTo(command)
    }

    suspend fun assertClose() {
        assertThat(closeCalls.awaitItem()).isEqualTo(Unit)
    }

    fun ensureAllEventsConsumed() {
        connectCalls.ensureAllEventsConsumed()
        transceiveCalls.ensureAllEventsConsumed()
        closeCalls.ensureAllEventsConsumed()
    }
}

package com.stripe.android.common.nfcscan

import android.nfc.Tag
import android.nfc.tech.IsoDep
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class FakeIsoDep(
    val tag: Tag = mock(),
) {
    private var transceiveResults: List<ByteArray> = emptyList()
    private var transceiveResultIndex = 0

    val wrappedInstance: IsoDep = mock()

    init {
        whenever(wrappedInstance.transceive(any())).thenAnswer {
            if (transceiveResultIndex < transceiveResults.size) {
                transceiveResults[transceiveResultIndex++]
            } else {
                byteArrayOf(0x69.toByte(), 0x82.toByte())
            }
        }

        doAnswer { null }.whenever(wrappedInstance).connect()
        doAnswer { null }.whenever(wrappedInstance).close()
    }

    fun setTransceiveResponses(
        transceiveResults: List<ByteArray>,
    ) {
        this.transceiveResults = transceiveResults
        this.transceiveResultIndex = 0
    }
}

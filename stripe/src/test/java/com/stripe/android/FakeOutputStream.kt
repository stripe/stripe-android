package com.stripe.android

import java.io.OutputStream

internal class FakeOutputStream : OutputStream() {
    var writtenBytesSize: Int = 0

    override fun write(b: Int) {
        write(
            ByteArray(1).apply {
                set(0, b.toByte())
            }
        )
    }

    override fun write(b: ByteArray) {
        writtenBytesSize += b.size
    }
}

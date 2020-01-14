package com.stripe.android

import java.io.OutputStream

internal class FakeOutputStream : OutputStream() {
    var writtenBytesSize: Int = 0

    override fun write(b: Int) {
    }

    override fun write(b: ByteArray) {
        writtenBytesSize += b.size
    }
}

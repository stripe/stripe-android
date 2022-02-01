package com.stripe.android.stripe3ds2.security

import kotlin.test.Test
import kotlin.test.assertTrue

class TransactionEncrypterTest {

    @Test
    fun cryptoGetGcmIvStoA_shouldReturnCorrectValue() {
        val counter: Byte = 10
        val expected = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10)
        assertTrue(
            TransactionEncrypter.Crypto.getGcmIvStoA(96, counter).contentEquals(expected)
        )
    }
}

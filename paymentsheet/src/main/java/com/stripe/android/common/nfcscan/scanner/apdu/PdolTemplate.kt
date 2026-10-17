package com.stripe.android.common.nfcscan.scanner.apdu

internal sealed interface PdolTemplate {
    data object None : PdolTemplate

    data class Available(val data: ByteArray) : PdolTemplate {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            val available = other as Available

            return data.contentEquals(available.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }
}

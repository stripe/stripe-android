package com.stripe.android.common.nfcscan.apdu.commands

internal class ApplicationIdentifier(
    val value: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApplicationIdentifier) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun toString(): String {
        return value.toHexString().uppercase()
    }
}

package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.UUID

@Parcelize
data class SdkTransactionId internal constructor(
    val value: String
) : Parcelable, Serializable {
    internal constructor(uuidValue: UUID) : this(
        uuidValue.toString()
    )

    override fun toString(): String = value

    companion object {
        fun create() = SdkTransactionId(UUID.randomUUID())
    }
}

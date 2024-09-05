package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import com.stripe.android.stripe3ds2.transactions.ErrorData
import kotlinx.parcelize.Parcelize

/**
 * The com.ults.samplesdk.RuntimeErrorEvent class shall hold details of run-time errors that are
 * encountered by the 3DS SDK during authentication.
 *
 *
 * Note: A run-time error is not the same as a protocol error. For information about protocol
 * errors, refer to Class ProtocolErrorEvent.
 *
 *
 * The implementer shall incorporate code that handles run-time errors. The following are examples
 * of run-time errors:
 *
 *  * ACS is unreachable.
 *  * Unparseable message.
 *  * Network issues.
 *
 */
@Parcelize
data class RuntimeErrorEvent(
    val errorCode: String,
    val errorMessage: String
) : Parcelable {
    internal constructor(throwable: Throwable) : this(
        throwable.javaClass.simpleName,
        throwable.message.orEmpty()
    )

    internal constructor(errorData: ErrorData) : this(
        errorData.errorCode,
        errorData.errorDetail
    )
}

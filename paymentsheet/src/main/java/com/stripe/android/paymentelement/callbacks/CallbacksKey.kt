package com.stripe.android.paymentelement.callbacks

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
internal sealed interface CallbacksKey : Parcelable {
    val key: String
}

@Parcelize
internal data class LifecycleCallbacksKey(
    override val key: String,
    val lifecycleId: UUID,
) : CallbacksKey

@Parcelize
internal data class UnscopedCallbacksKey(
    override val key: String,
) : CallbacksKey

package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Checkout private constructor(
    var state: State
) {
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        suspend fun configure(
            checkoutSessionClientSecret: String,
        ): Result<Checkout> {
            return Result.success(Checkout(State(checkoutSessionClientSecret)))
        }

        fun createWithState(
            state: State,
        ): Checkout {
            return Checkout(state)
        }
    }

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(internal val checkoutSessionClientSecret: String) : Parcelable
}

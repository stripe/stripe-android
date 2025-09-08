package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Payment method options containing configuration and collected data.
 */
@Parcelize
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(cttsai-stripe): should make the data class a normal class in next major release
data class PaymentMethodOptions internal constructor(
    /**
     * Card-specific options.
     */
    @JvmField val card: Card?,

    ) : StripeModel {

    @Parcelize
    @Poko
    class Card internal constructor(
        /**
         * CVC token for the card.
         */
        @JvmField val cvcToken: String?,

        ) : Parcelable

}

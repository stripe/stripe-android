package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpressCheckoutElement internal constructor() {

    @Composable
    fun Content() {
        TODO("Not yet implemented")
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var visibility: PaymentSheet.WalletButtonsConfiguration.Visibility =
            PaymentSheet.WalletButtonsConfiguration.Visibility()

        fun visibility(
            visibility: PaymentSheet.WalletButtonsConfiguration.Visibility
        ): Configuration = apply {
            this.visibility = visibility
        }

        @Parcelize
        internal data class State(
            val visibility: PaymentSheet.WalletButtonsConfiguration.Visibility,
        ) : Parcelable

        internal fun build(): State = State(
            visibility = visibility,
        )
    }
}

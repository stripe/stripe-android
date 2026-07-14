package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.checkout.ece.DefaultExpressCheckoutElementInteractor
import com.stripe.android.checkout.ece.ExpressCheckoutElementContent
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpressCheckoutElement internal constructor(
    private val interactorFactory: ExpressCheckoutElementInteractor.Factory =
        DefaultExpressCheckoutElementInteractor.Factory,
) {

    @Composable
    fun Content() {
        val interactor = remember(interactorFactory) { interactorFactory.create() }
        ExpressCheckoutElementContent(interactor = interactor)
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

package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.checkout.ece.ExpressCheckoutElementContent
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpressCheckoutElement internal constructor(
    private val interactor: ExpressCheckoutElementInteractor,
) {

    @Composable
    fun Content() {
        ExpressCheckoutElementContent(interactor = interactor)
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class LinkVisibility {
            Auto,
            Never,
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class GooglePayVisibility {
            Auto,
            Never,
        }

        private var linkVisibility: LinkVisibility = LinkVisibility.Auto
        private var googlePayVisibility: GooglePayVisibility = GooglePayVisibility.Auto

        fun linkVisibility(
            linkVisibility: LinkVisibility
        ): Configuration = apply {
            this.linkVisibility = linkVisibility
        }

        fun googlePayVisibility(
            googlePayVisibility: GooglePayVisibility
        ): Configuration = apply {
            this.googlePayVisibility = googlePayVisibility
        }

        @Parcelize
        internal data class State(
            val linkVisibility: LinkVisibility,
            val googlePayVisibility: GooglePayVisibility,
        ) : Parcelable

        internal fun build(): State = State(
            linkVisibility = linkVisibility,
            googlePayVisibility = googlePayVisibility,
        )
    }
}

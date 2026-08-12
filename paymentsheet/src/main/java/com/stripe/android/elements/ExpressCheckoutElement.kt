package com.stripe.android.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.elements.ece.ExpressCheckoutElementContent
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractor
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpressCheckoutElement @Inject internal constructor(
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
        @CheckoutSessionPreview
        enum class LinkVisibility {
            Auto,
            Never,
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @CheckoutSessionPreview
        enum class GooglePayVisibility {
            Auto,
            Never,
        }

        private var linkVisibility: LinkVisibility = LinkVisibility.Auto
        private var googlePayVisibility: GooglePayVisibility = GooglePayVisibility.Auto

        private var shippingAddressRequired: Boolean = false

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

        fun shippingAddressRequired(
            shippingAddressRequired: Boolean,
        ): Configuration = apply {
            this.shippingAddressRequired = shippingAddressRequired
        }

        @Parcelize
        internal data class State(
            val linkVisibility: LinkVisibility,
            val googlePayVisibility: GooglePayVisibility,
            val shippingAddressRequired: Boolean,
        ) : Parcelable

        internal fun build(): State = State(
            linkVisibility = linkVisibility,
            googlePayVisibility = googlePayVisibility,
            shippingAddressRequired = shippingAddressRequired,
        )
    }
}

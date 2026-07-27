package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.stripe.android.checkout.ece.ExpressCheckoutElementContent
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
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

    /** Payment methods supported by the Express Checkout Element. */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class PaymentMethod {
        GooglePay,
        Link,
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {

        /** Configuration for the payment methods shown by the element. */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class PaymentMethods {
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

            private var link: LinkVisibility = LinkVisibility.Auto
            private var googlePay: GooglePayVisibility = GooglePayVisibility.Auto

            fun link(visibility: LinkVisibility): PaymentMethods = apply {
                link = visibility
            }

            fun googlePay(visibility: GooglePayVisibility): PaymentMethods = apply {
                googlePay = visibility
            }

            @Parcelize
            internal data class State(
                val link: LinkVisibility,
                val googlePay: GooglePayVisibility,
            ) : Parcelable

            internal fun build(): State = State(link = link, googlePay = googlePay)
        }

        /** Determines how wallet buttons are arranged. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class ButtonOrientation {
            Horizontal,
            Vertical,
        }

        private var paymentMethods: PaymentMethods = PaymentMethods()
        private var shippingAddressRequired: Boolean = false
        private var allowedShippingCountries: Set<String> = emptySet()
        private var buttonHeight: Dp? = null
        private var buttonOrientation: ButtonOrientation = ButtonOrientation.Vertical

        /**
         * Configures which wallet payment methods are displayed.
         *
         * This is the equivalent of the web Express Checkout Element's `paymentMethods` option.
         */
        fun paymentMethods(paymentMethods: PaymentMethods): Configuration = apply {
            this.paymentMethods = paymentMethods
        }

        /** Requests a shipping address from the selected wallet when `true`. */
        fun shippingAddressRequired(shippingAddressRequired: Boolean): Configuration = apply {
            this.shippingAddressRequired = shippingAddressRequired
        }

        /**
         * Restricts wallet shipping addresses to ISO 3166-1 alpha-2 country codes.
         *
         * This option only applies when shipping address collection is required.
         */
        fun allowedShippingCountries(allowedShippingCountries: Set<String>): Configuration = apply {
            this.allowedShippingCountries = allowedShippingCountries.map { it.uppercase() }.toSet()
        }

        /** Sets the height of wallet buttons. */
        fun buttonHeight(buttonHeight: Dp): Configuration = apply {
            require(buttonHeight > 0.dp) { "buttonHeight must be greater than zero." }
            this.buttonHeight = buttonHeight
        }

        /** Sets whether wallet buttons are arranged horizontally or vertically. */
        fun buttonOrientation(buttonOrientation: ButtonOrientation): Configuration = apply {
            this.buttonOrientation = buttonOrientation
        }

        @Parcelize
        internal data class State(
            val paymentMethods: PaymentMethods.State,
            val shippingAddressRequired: Boolean,
            val allowedShippingCountries: Set<String>,
            val buttonHeight: Float?,
            val buttonOrientation: ButtonOrientation,
        ) : Parcelable

        internal fun build(): State = State(
            paymentMethods = paymentMethods.build(),
            shippingAddressRequired = shippingAddressRequired,
            allowedShippingCountries = allowedShippingCountries,
            buttonHeight = buttonHeight?.value,
            buttonOrientation = buttonOrientation,
        )
    }
}

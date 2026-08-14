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

    /**
     * A composable function which displays express buttons such as Google Pay and Link.
     *
     * Customers can tap an express button to complete payment with that payment method.
     */
    @Composable
    fun Content() {
        ExpressCheckoutElementContent(interactor = interactor)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    /** Payment methods supported by the Express Checkout Element. */
    abstract class PaymentMethod private constructor() {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class GooglePay internal constructor() : PaymentMethod()

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Link internal constructor() : PaymentMethod()
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

        /**
         * Configuration related to Google Pay.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @CheckoutSessionPreview
        class GooglePayConfiguration {

            /**
             * Display configuration for Google Pay.
             */
            enum class Display {
                /**
                 * Google Pay will be displayed when available.
                 */
                Automatic,

                /**
                 * Google Pay will never be displayed.
                 */
                Never,
            }

            private var display: Display = Display.Automatic
            private var label: String? = null
            private var buttonType: ButtonType = ButtonType.Pay
            private var additionalEnabledNetworks: List<String> = emptyList()

            /**
             * Sets the display configuration for Google Pay.
             *
             * @param display The display configuration for Google Pay.
             */
            fun display(display: Display): GooglePayConfiguration = apply {
                this.display = display
            }

            /**
             * Sets the label displayed with the amount.
             *
             * @param label An optional label to display with the amount. Google Pay may or may not display
             * this label depending on its own internal logic. Defaults to a generic label if none is
             * provided.
             */
            fun label(label: String): GooglePayConfiguration = apply {
                this.label = label
            }

            /**
             * Sets the Google Pay button type.
             *
             * @param buttonType The Google Pay button type to use. Set to "Pay" by default. See
             * [Google's documentation](https://developers.google.com/pay/api/android/reference/request-objects#ButtonOptions)
             * for more information on button types.
             */
            fun buttonType(buttonType: ButtonType): GooglePayConfiguration = apply {
                this.buttonType = buttonType
            }

            /**
             * Sets additional card networks that Google Pay can display.
             *
             * @param additionalEnabledNetworks An optional List<String> to signal GooglePay to
             * display additional enabled networks (e.g. 'INTERAC')
             */
            fun additionalEnabledNetworks(
                additionalEnabledNetworks: List<String>
            ): GooglePayConfiguration = apply {
                this.additionalEnabledNetworks = additionalEnabledNetworks
            }

            @CheckoutSessionPreview
            /**
             * Google Pay button type options
             *
             * See
             * [Google's documentation](https://developers.google.com/pay/api/android/reference/request-objects#ButtonOptions)
             * for more information on button types.
             */
            enum class ButtonType {
                /**
                 * Displays "Buy with" alongside the Google Pay logo.
                 */
                Buy,

                /**
                 * Displays "Book with" alongside the Google Pay logo.
                 */
                Book,

                /**
                 * Displays "Checkout with" alongside the Google Pay logo.
                 */
                Checkout,

                /**
                 * Displays "Donate with" alongside the Google Pay logo.
                 */
                Donate,

                /**
                 * Displays "Order with" alongside the Google Pay logo.
                 */
                Order,

                /**
                 * Displays "Pay with" alongside the Google Pay logo.
                 */
                Pay,

                /**
                 * Displays "Subscribe with" alongside the Google Pay logo.
                 */
                Subscribe,

                /**
                 * Displays only the Google Pay logo.
                 */
                Plain
            }

            @Parcelize
            internal data class State(
                val display: Display,
                val label: String?,
                val buttonType: ButtonType,
                val additionalEnabledNetworks: List<String>,
            ) : Parcelable

            internal fun build(): State = State(
                display = display,
                label = label,
                buttonType = buttonType,
                additionalEnabledNetworks = additionalEnabledNetworks,
            )
        }
        private var linkVisibility: LinkVisibility = LinkVisibility.Auto
        private var googlePayConfiguration: GooglePayConfiguration = GooglePayConfiguration()

        private var shippingAddressRequired: Boolean = false

        fun linkVisibility(
            linkVisibility: LinkVisibility
        ): Configuration = apply {
            this.linkVisibility = linkVisibility
        }

        fun googlePayConfiguration(
            googlePayConfiguration: GooglePayConfiguration
        ): Configuration = apply {
            this.googlePayConfiguration = googlePayConfiguration
        }

        fun shippingAddressRequired(
            shippingAddressRequired: Boolean,
        ): Configuration = apply {
            this.shippingAddressRequired = shippingAddressRequired
        }

        @Parcelize
        internal data class State(
            val linkVisibility: LinkVisibility,
            val googlePayConfiguration: GooglePayConfiguration.State,
            val shippingAddressRequired: Boolean,
        ) : Parcelable

        internal fun build(): State = State(
            linkVisibility = linkVisibility,
            googlePayConfiguration = googlePayConfiguration.build(),
            shippingAddressRequired = shippingAddressRequired,
        )
    }
}

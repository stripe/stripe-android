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

        /**
         * Configuration for how billing details are collected during checkout.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class BillingDetailsCollectionConfiguration {
            private var name: CollectionMode = CollectionMode.Automatic
            private var email: CollectionMode = CollectionMode.Automatic
            private var address: AddressCollectionMode = AddressCollectionMode.Automatic

            /**
             * Billing details fields collection options.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class CollectionMode {
                /**
                 * The field will be collected depending on the Payment Method's requirements.
                 */
                Automatic,

                /**
                 * The field will never be collected.
                 * If this field is required by the Payment Method, you must provide it as part of
                 * the default billing details.
                 */
                Never,

                /**
                 * The field will always be collected, even if it isn't required for the Payment
                 * Method.
                 */
                Always,
            }

            /**
             * Billing address collection options.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class AddressCollectionMode {
                /**
                 * Only the fields required by the Payment Method will be collected, this may be
                 * none.
                 */
                Automatic,

                /**
                 * Collect the full billing address, regardless of the Payment Method requirements.
                 */
                Full,

                // Note: a `Never` mode is intentionally omitted for the CheckoutSession private
                // preview — suppressing billing collection is not supported with a CheckoutSession.
                // It can be added at public preview/GA if that use case is supported.
            }

            /** How to collect the name field. */
            fun name(name: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.name = name
            }

            /** How to collect the email field. */
            fun email(email: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.email = email
            }

            /** How to collect the billing address. */
            fun address(address: AddressCollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.address = address
            }

            @Parcelize
            internal data class State(
                val name: CollectionMode,
                val email: CollectionMode,
                val address: AddressCollectionMode,
            ) : Parcelable

            internal fun build(): State = State(
                name = name,
                email = email,
                address = address,
            )
        }

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
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()

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

        /** Sets how billing details are collected when displaying payment methods. */
        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        @Parcelize
        internal data class State(
            val linkVisibility: LinkVisibility,
            val googlePayConfiguration: GooglePayConfiguration.State,
            val shippingAddressRequired: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
        ) : Parcelable

        internal fun build(): State = State(
            linkVisibility = linkVisibility,
            googlePayConfiguration = googlePayConfiguration.build(),
            shippingAddressRequired = shippingAddressRequired,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
        )
    }
}

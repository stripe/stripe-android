package com.stripe.android.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import com.stripe.android.LinkDisallowFundingSourceCreationPreview
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

        /**
         * Configuration related to Link.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class LinkConfiguration {
            private var display: Display = Display.Automatic
            private var collectMissingBillingDetailsForExistingPaymentMethods: Boolean = true
            private var disallowFundingSourceCreation: Set<String> = emptySet()

            /**
             * Display configuration for Link.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class Display {
                /**
                 * Link will be displayed when available.
                 */
                Automatic,

                /**
                 * Link will never be displayed.
                 */
                Never,

                /**
                 * Link remains enabled but its button or row is hidden from the payment element UI.
                 */
                WalletButtonHidden,
            }

            /** Sets the display configuration for Link. */
            fun display(display: Display): LinkConfiguration = apply {
                this.display = display
            }

            /**
             * Sets whether Link collects missing billing details for existing payment methods.
             */
            @CollectMissingLinkBillingDetailsPreview
            fun collectMissingBillingDetailsForExistingPaymentMethods(
                collectMissingBillingDetailsForExistingPaymentMethods: Boolean,
            ): LinkConfiguration = apply {
                this.collectMissingBillingDetailsForExistingPaymentMethods =
                    collectMissingBillingDetailsForExistingPaymentMethods
            }

            /**
             * Sets the funding source types that Link must not create.
             */
            @LinkDisallowFundingSourceCreationPreview
            fun disallowFundingSourceCreation(
                disallowFundingSourceCreation: Set<String>,
            ): LinkConfiguration = apply {
                this.disallowFundingSourceCreation = disallowFundingSourceCreation
            }

            @Parcelize
            internal data class State(
                val display: Display,
                val collectMissingBillingDetailsForExistingPaymentMethods: Boolean,
                val disallowFundingSourceCreation: Set<String>,
            ) : Parcelable

            internal fun build(): State = State(
                display = display,
                collectMissingBillingDetailsForExistingPaymentMethods =
                    collectMissingBillingDetailsForExistingPaymentMethods,
                disallowFundingSourceCreation = disallowFundingSourceCreation.toSet(),
            )
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
        private var linkConfiguration: LinkConfiguration = LinkConfiguration()
        private var googlePayConfiguration: GooglePayConfiguration = GooglePayConfiguration()

        private var shippingAddressRequired: Boolean = false
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()
        private var paymentMethodOrder: List<PaymentMethod> = emptyList()

        /** Sets the configuration for Link. */
        fun linkConfiguration(
            configuration: LinkConfiguration,
        ): Configuration = apply {
            this.linkConfiguration = configuration
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

        /**
         * Sets the order in which express payment methods are displayed.
         *
         * By default, the Express Checkout Element uses dynamic ordering. Payment methods omitted
         * from [paymentMethodOrder] are displayed after the specified payment methods. Payment
         * methods that are unavailable are ignored.
         */
        fun paymentMethodOrder(
            paymentMethodOrder: List<PaymentMethod>,
        ): Configuration = apply {
            this.paymentMethodOrder = paymentMethodOrder
        }

        @Parcelize
        internal data class State(
            val linkConfiguration: LinkConfiguration.State,
            val googlePayConfiguration: GooglePayConfiguration.State,
            val shippingAddressRequired: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
            val paymentMethodOrder: List<PaymentMethodType>,
        ) : Parcelable

        internal enum class PaymentMethodType {
            GooglePay,
            Link,
        }

        internal fun build(): State = State(
            linkConfiguration = linkConfiguration.build(),
            googlePayConfiguration = googlePayConfiguration.build(),
            shippingAddressRequired = shippingAddressRequired,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
            paymentMethodOrder = paymentMethodOrder.map { paymentMethod ->
                when (paymentMethod) {
                    is PaymentMethod.GooglePay -> PaymentMethodType.GooglePay
                    is PaymentMethod.Link -> PaymentMethodType.Link
                    else -> error("Unsupported payment method: ${paymentMethod::class.java.name}")
                }
            },
        )
    }
}

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
        class GooglePay internal constructor() : PaymentMethod() {
            override fun equals(other: Any?): Boolean = other is GooglePay

            override fun hashCode(): Int = GooglePay::class.java.hashCode()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Link internal constructor() : PaymentMethod() {
            override fun equals(other: Any?): Boolean = other is Link

            override fun hashCode(): Int = Link::class.java.hashCode()
        }
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
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

            internal fun build(): CheckoutGooglePayConfiguration = CheckoutGooglePayConfiguration(
                display = display.asCheckout(),
                label = label,
                buttonType = buttonType.asCheckout(),
                additionalEnabledNetworks = additionalEnabledNetworks,
            )
        }

        /** Appearance configuration for the Express Checkout Element. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @CheckoutSessionPreview
        class Appearance {
            private var buttonLayout: ButtonLayout = ButtonLayout()
            private var buttonTheme: ButtonTheme = ButtonTheme.Automatic

            /** Themes for express buttons. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @CheckoutSessionPreview
            enum class ButtonTheme {
                /** Light theme which contrasts with a dark background. */
                Light,

                /** Dark theme which contrasts with a light background. */
                Dark,

                /** Automatic theme which contrasts with the current system theme. */
                Automatic,
            }

            /** Determines how express buttons are arranged. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @CheckoutSessionPreview
            class ButtonLayout {
                private var maxColumns: Int? = null
                private var maxRows: Int? = null

                /**
                 * Sets the maximum number of columns the Express Checkout Element can use.
                 *
                 * Defaults to null, meaning unlimited.
                 */
                fun maxColumns(maxColumns: Int?): ButtonLayout = apply {
                    require(maxColumns == null || maxColumns > 0) {
                        "maxColumns must be greater than zero or null."
                    }
                    this.maxColumns = maxColumns
                }

                /**
                 * Sets the maximum number of rows the Express Checkout Element can use.
                 *
                 * Defaults to null, meaning unlimited.
                 */
                fun maxRows(maxRows: Int?): ButtonLayout = apply {
                    require(maxRows == null || maxRows > 0) {
                        "maxRows must be greater than zero or null."
                    }
                    this.maxRows = maxRows
                }

                @Parcelize
                internal data class State(
                    val maxColumns: Int?,
                    val maxRows: Int?,
                ) : Parcelable

                internal fun build(): State = State(
                    maxColumns = maxColumns,
                    maxRows = maxRows,
                )
            }

            /** Configures how payment methods are arranged within the Express Checkout Element. */
            fun buttonLayout(buttonLayout: ButtonLayout): Appearance = apply {
                this.buttonLayout = buttonLayout
            }

            /** Sets the theme of the express buttons. */
            fun buttonTheme(buttonTheme: ButtonTheme): Appearance = apply {
                this.buttonTheme = buttonTheme
            }

            @Parcelize
            internal data class State(
                val buttonLayout: ButtonLayout.State,
                val buttonTheme: ButtonTheme,
            ) : Parcelable

            internal fun build(): State = State(
                buttonLayout = buttonLayout.build(),
                buttonTheme = buttonTheme,
            )
        }

        private var linkConfiguration: LinkConfiguration = LinkConfiguration()
        private var googlePayConfiguration: GooglePayConfiguration = GooglePayConfiguration()

        private var emailRequired: Boolean = false
        private var paymentMethodOrder: List<String> = emptyList()
        private var appearance: Appearance = Appearance()

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

        /**
         * Sets whether an email address is required.
         *
         * @param emailRequired If true, the customer's email will be collected.
         */
        fun emailRequired(
            emailRequired: Boolean,
        ): Configuration = apply {
            this.emailRequired = emailRequired
        }

        /**
         * Sets the order in which express payment methods are displayed.
         *
         * Supported values are `"google_pay"` and `"link"`.
         *
         * By default, the Express Checkout Element uses dynamic ordering. Payment methods omitted
         * from [paymentMethodOrder] are displayed after the specified payment methods. Payment
         * methods that are unavailable or invalid are ignored.
         */
        fun paymentMethodOrder(
            paymentMethodOrder: List<String>,
        ): Configuration = apply {
            this.paymentMethodOrder = paymentMethodOrder
        }

        /** Sets the appearance of the Express Checkout Element. */
        fun appearance(appearance: Appearance): Configuration = apply {
            this.appearance = appearance
        }

        @Parcelize
        internal data class State(
            val linkConfiguration: LinkConfiguration.State,
            val googlePayConfiguration: CheckoutGooglePayConfiguration,
            val emailRequired: Boolean,
            val paymentMethodOrder: List<PaymentMethodType>,
            val appearance: Appearance.State,
        ) : Parcelable

        internal enum class PaymentMethodType {
            GooglePay,
            Link,
        }

        internal fun build(): State = State(
            linkConfiguration = linkConfiguration.build(),
            googlePayConfiguration = googlePayConfiguration.build(),
            emailRequired = emailRequired,
            paymentMethodOrder = paymentMethodOrder.mapNotNull { paymentMethod ->
                when (paymentMethod) {
                    "google_pay" -> PaymentMethodType.GooglePay
                    "link" -> PaymentMethodType.Link
                    else -> null
                }
            },
            appearance = appearance.build(),
        )
    }
}

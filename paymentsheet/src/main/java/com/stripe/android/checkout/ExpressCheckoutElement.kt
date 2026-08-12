package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import com.stripe.android.LinkDisallowFundingSourceCreationPreview
import com.stripe.android.checkout.ece.ExpressCheckoutElementContent
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import javax.inject.Inject

@CheckoutSessionPreview
class ExpressCheckoutElement @Inject internal constructor(
    private val interactor: ExpressCheckoutElementInteractor,
) {

    @Composable
    fun Content() {
        ExpressCheckoutElementContent(interactor = interactor)
    }

    /** Payment methods supported by the Express Checkout Element. */
    @CheckoutSessionPreview
    abstract class PaymentMethod private constructor() : Parcelable {

        @Parcelize
        class GooglePay internal constructor() : PaymentMethod()

        @Parcelize
        class Link internal constructor() : PaymentMethod()
    }

    @CheckoutSessionPreview
    class Configuration {

        /**
         * Configuration for how billing details are collected during checkout.
         */
        class BillingDetailsCollectionConfiguration {
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

            private var name: CollectionMode = CollectionMode.Automatic
            private var phone: CollectionMode = CollectionMode.Automatic
            private var email: CollectionMode = CollectionMode.Automatic
            private var address: AddressCollectionMode = AddressCollectionMode.Automatic

            /** How to collect the name field. */
            fun name(name: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.name = name
            }

            /** How to collect the phone field. */
            fun phone(phone: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.phone = phone
            }

            /** How to collect the email field. */
            fun email(email: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.email = email
            }

            /** How to collect the billing address. */
            fun address(address: AddressCollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.address = address
            }
        }

        /** Configuration related to Link. */
        class LinkConfiguration {
            /**
             * Display configuration for Link
             */
            enum class Display {
                /**
                 * Link will be displayed when available.
                 */
                Automatic,

                /**
                 * Link will never be displayed.
                 */
                Never,
            }

            /** Sets the display configuration for Link. */
            fun display(display: Display): LinkConfiguration = apply {
                throw NotImplementedError()
            }

            /**
             * Sets whether Link collects missing billing details for existing payment methods.
             */
            @CollectMissingLinkBillingDetailsPreview
            fun collectMissingBillingDetailsForExistingPaymentMethods(
                collectMissingBillingDetailsForExistingPaymentMethods: Boolean
            ): LinkConfiguration = apply {
                throw NotImplementedError()
            }

            /**
             * Sets the funding source types that Link must not create.
             */
            @LinkDisallowFundingSourceCreationPreview
            fun disallowFundingSourceCreation(disallowFundingSourceCreation: Set<String>): LinkConfiguration = apply {
                throw NotImplementedError()
            }
        }

        /**
         * Configuration related to Google Pay.
         *
         * @param environment The Google Pay environment to use. See
         * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
         * for more information.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class GooglePayConfiguration {

            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class Environment {
                Production,
                Test,
            }

            enum class Display {
                Automatic,
                Never
            }

            private var label: String? = null
            private var buttonType: ButtonType = ButtonType.Pay
            private var additionalEnabledNetworks: List<String> = emptyList()

            private var environment: Environment? = null

            private var display: Display = Display.Automatic

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
             * @param environment  The Google Pay environment to use. See
             * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
             * for more information.
             *
             * If not set, we will use [Environment.Test] in test mode and [Environment.Production] otherwise.
             */
            fun environment(environment: Environment): GooglePayConfiguration = apply {
                this.environment = environment
            }

            /**
             * Sets the display configuration for Google Pay.
             *
             * @param display The display configuration for Google Pay.
             */
            fun display(display: Display): GooglePayConfiguration = apply {
                this.display = display
            }

            /**
             * Sets the Google Pay button type.
             *
             * @param buttonType The Google Pay button type to use. Set to "Pay" by default. See
             * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
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

            @Parcelize
            internal data class State(
                val environment: Environment,
                val label: String?,
                val buttonType: ButtonType,
                val additionalEnabledNetworks: List<String>,
            ) : Parcelable

            internal fun build(): State = State(
                environment = environment ?: Environment.Production, // TODO: we'd really infer this during PEL based on intent live mode or test mode.
                label = label,
                buttonType = buttonType,
                additionalEnabledNetworks = additionalEnabledNetworks.toList(),
            )


            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
        }


        private var paymentMethodOrder: List<PaymentMethod> = emptyList()
        private var shippingAddressRequired: Boolean = false
        // TODO: note on API review that this is different from the web ECE API.
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()
        private var appearance: Appearance = Appearance()

        /**
         * Sets whether a shipping address is required.
         *
         * @param shippingAddressRequired If true, the customer's shipping address will be collected.
         * */
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
         * By default, the Express Checkout Element uses a dynamic ordering that optimizes payment method display for
         * each user.
         *
         * You can override the default order in which payment methods display in the Express Checkout Element with a
         * list of [PaymentMethod]s.
         *
         * If there are payment methods that will show that are not specified in paymentMethodOrder, they display after
         * the payment methods you specify. If you specify payment methods that will not show, they are ignored.
         */
        fun paymentMethodOrder(paymentMethodOrder: List<PaymentMethod>): Configuration = apply {
            this.paymentMethodOrder = paymentMethodOrder
        }

        /**
         * Sets the configuration for Google Pay.
         *
         * Required to display Google Pay.
         */
        fun googlePayConfiguration(googlePayConfiguration: GooglePayConfiguration): Configuration {
            throw NotImplementedError()
        }

        /** Sets the configuration for Link. */
        fun linkConfiguration(linkConfiguration: LinkConfiguration): Configuration {
            throw NotImplementedError()
        }

        /** Sets the appearance of the Express Checkout Element. */
        fun appearance(
            appearance: Appearance
        ): Configuration = apply {
            this.appearance = appearance
        }


        class Appearance {
            private var buttonHeight: Dp? = null
            private var buttonLayout: ButtonLayout = ButtonLayout()

            /** Determines how express buttons are arranged. */
            class ButtonLayout {
                private var maxColumns: Int? = null
                private var maxRows: Int? = null

                /** Sets the maximum number of columns the Express Checkout Element can use to render.
                 *
                 * Defaults to null, meaning unlimited. */
                fun maxColumns(
                    maxColumns: Int?
                ): ButtonLayout = apply {
                    this.maxColumns = maxColumns
                }

                /** Sets the maximum number of rows the Express Checkout Element can use to render.
                 *
                 * Defaults to null, meaning unlimited. */
                fun maxRows(
                    maxRows: Int?
                ): ButtonLayout = apply {
                    this.maxRows = maxRows
                }

                @Parcelize
                internal data class State(
                    val maxColumns: Int?,
                    val maxRows: Int?,
                ) : Parcelable

                internal fun build(): State = State(maxColumns = maxColumns, maxRows = maxRows)
            }

            /** Themes for express buttons. */
            enum class ButtonTheme {
                /** Light theme which contrasts with a dark background. */
                Light,

                /** Dark theme which contrasts with a light background. */
                Dark,

                /** Automatic theme which contrasts with background depending on system theme. */
                Automatic,
            }

            /** Sets the layout of the express buttons. */
            fun buttonLayout(buttonLayout: ButtonLayout): Appearance = apply {
                this.buttonLayout = buttonLayout
            }

            /** Sets the theme of the express buttons. */
            fun buttonTheme(buttonTheme: ButtonTheme): Appearance {
                throw NotImplementedError()
            }

            @Parcelize
            internal data class State(
                val buttonHeight: @RawValue Dp?,
                val buttonLayout: ButtonLayout.State,
            ) : Parcelable

            internal fun build(): State = State(
                buttonHeight = buttonHeight,
                buttonLayout = buttonLayout.build(),
            )
        }


        @Parcelize
        internal data class State(
            val paymentMethodOrder: List<PaymentMethod>,
            val shippingAddressRequired: Boolean,
            val appearance: Appearance.State,
        ) : Parcelable

        internal fun build(): State = State(
            paymentMethodOrder = paymentMethodOrder,
            shippingAddressRequired = shippingAddressRequired,
            appearance = appearance.build(),
        )
    }
}

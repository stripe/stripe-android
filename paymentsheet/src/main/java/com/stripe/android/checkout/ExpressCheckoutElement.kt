package com.stripe.android.checkout

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    enum class PaymentMethod {
        GooglePay,
        Link,
    }

    @CheckoutSessionPreview
    class Configuration {


        private var paymentMethods: PaymentMethods = PaymentMethods()
        private var paymentMethodOrder: List<PaymentMethod> = emptyList()
        private var shippingAddressRequired: Boolean = false
        // TODO: note on API review that this is different from the web ECE API.
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()
        private var appearance: Appearance = Appearance()

        fun shippingAddressRequired(
            shippingAddressRequired: Boolean,
        ): Configuration = apply {
            this.shippingAddressRequired = shippingAddressRequired
        }

        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        /**
         * Configures which payment methods are displayed.
         *
         * By default, the Express Checkout Element displays all payment methods possible as a result of your Dashboard
         * configuration and device capabilities. This is the auto behavior.
         *
         * If you don't want to show a given payment method as a payment option, set its property in paymentMethods to
         * never.
         */
        fun paymentMethods(paymentMethods: PaymentMethods): Configuration = apply {
            this.paymentMethods = paymentMethods
        }

        /**
         * Configures the order express payment methods are displayed in.
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

        fun appearance(
            appearance: Appearance
        ): Configuration = apply {
            this.appearance = appearance
        }


        /** Determines how express buttons are arranged. */
        class ButtonLayout {
            private var maxColumns: Int? = null
            private var maxRows: Int? = null

            fun maxColumns(
                maxColumns: Int?
            ): ButtonLayout = apply {
                this.maxColumns = maxColumns
            }

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

        @CheckoutSessionPreview
        class PaymentMethods {
            enum class LinkVisibility {
                Auto,
                Never,
            }

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

        class Appearance {
            private var buttonHeight: Dp? = null
            private var buttonLayout: ButtonLayout = ButtonLayout()

            // TODO: do we need a corner radius or padding appearance params?

            // TODO: add a button type here? No -- but call out in API review.
            // TODO: also note that we're going to skip button theme.

            /** Sets the height of express buttons. */
            fun buttonHeight(buttonHeight: Dp): Appearance = apply {
                require(buttonHeight > 0.dp) { "buttonHeight must be greater than zero." }
                this.buttonHeight = buttonHeight
            }

            /** Sets the layout of the express buttons. */
            fun buttonLayout(buttonLayout: ButtonLayout): Appearance = apply {
                this.buttonLayout = buttonLayout
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
            val paymentMethods: PaymentMethods.State,
            val paymentMethodOrder: List<PaymentMethod>,
            val shippingAddressRequired: Boolean,
            val appearance: Appearance.State,
        ) : Parcelable

        internal fun build(): State = State(
            paymentMethods = paymentMethods.build(),
            paymentMethodOrder = paymentMethodOrder,
            shippingAddressRequired = shippingAddressRequired,
            appearance = appearance.build(),
        )
    }
}

package com.stripe.android.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement @Inject internal constructor(
    private val contentHelper: EmbeddedContentHelper,
) {

    /**
     * A composable function that displays payment methods inline.
     *
     * It can present a sheet to collect more details or display saved payment methods.
     */
    @Composable
    fun PaymentOptionsContent() {
        val embeddedContent by contentHelper.embeddedContent.collectAsState()
        embeddedContent?.Content()
    }

    /**
     * Presents a sheet for the customer to select or manage their payment method.
     */
    fun presentPaymentOptions() {
        contentHelper.presentPaymentOptions()
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()
        private var paymentMethodLayout: PaymentMethodLayout = PaymentMethodLayout.Automatic

        /**
         * Controls whether [PaymentOptionsContent] displays mandate text below the payment methods.
         * Defaults to `true`.
         *
         * When set to `false`, you must display
         * [CheckoutController.Session.PaymentOptionDisplayData.mandateText] to the customer yourself,
         * near your "Buy" button, to comply with regulations.
         */
        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        /**
         * Sets how billing details are collected when displaying payment methods.
         */
        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        /**
         * The layout of payment methods in the sheet. Defaults to [PaymentMethodLayout.Automatic].
         *
         * Note: Only used if you call `presentPaymentOptions`.
         *
         * @see [PaymentMethodLayout] for the list of available layouts.
         */
        fun paymentMethodLayout(
            paymentMethodLayout: PaymentMethodLayout
        ): Configuration = apply {
            this.paymentMethodLayout = paymentMethodLayout
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
            val paymentMethodLayout: PaymentMethodLayout,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
            paymentMethodLayout = paymentMethodLayout,
        )

        /**
         * Configuration for how billing details are collected during checkout.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class BillingDetailsCollectionConfiguration {

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

            @Parcelize
            internal data class State(
                val name: CollectionMode,
                val phone: CollectionMode,
                val email: CollectionMode,
                val address: AddressCollectionMode,
            ) : Parcelable

            internal fun build(): State = State(
                name = name,
                phone = phone,
                email = email,
                address = address,
            )

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
        }
    }

    /**
     * The layout of payment methods.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class PaymentMethodLayout {
        /**
         * Payment methods are arranged horizontally.
         * Users can swipe left or right to navigate through different payment methods.
         */
        Horizontal,

        /**
         * Payment methods are arranged vertically.
         * Users can scroll up or down to navigate through different payment methods.
         */
        Vertical,

        /**
         * This lets Stripe choose the best layout for payment methods.
         */
        Automatic
    }
}

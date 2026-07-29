package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement @Inject internal constructor(
    private val contentHelper: EmbeddedContentHelper,
) {

    @Composable
    fun PaymentOptionsContent() {
        val embeddedContent by contentHelper.embeddedContent.collectAsState()
        embeddedContent?.Content()
    }

    fun presentPaymentOptions() {
        contentHelper.presentPaymentOptions()
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()
        private var appearance: PaymentSheet.Appearance = ConfigurationDefaults.appearance
        private var preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks
        private var paymentMethodOrder: List<String> = ConfigurationDefaults.paymentMethodOrder
        private var cardBrandAcceptance: PaymentSheet.CardBrandAcceptance =
            ConfigurationDefaults.cardBrandAcceptance
        private var opensCardScannerAutomatically: Boolean =
            ConfigurationDefaults.opensCardScannerAutomatically

        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        /**
         * Describes the appearance of the Payment Element.
         */
        fun appearance(
            appearance: PaymentSheet.Appearance
        ): Configuration = apply {
            this.appearance = appearance
        }

        /**
         * A list of preferred networks that should be used to process payments made with a co-branded card if
         * your user hasn't selected a network themselves.
         *
         * The first preferred network that matches any available network will be used. If no preferred network
         * is applicable, Stripe will select the network.
         */
        fun preferredNetworks(
            preferredNetworks: List<CardBrand>
        ): Configuration = apply {
            this.preferredNetworks = preferredNetworks
        }

        /**
         * By default, Stripe will use a dynamic ordering that optimizes payment method display for the customer.
         * You can override the default order in which payment methods are displayed with a list of payment
         * method types.
         *
         * See https://stripe.com/docs/api/payment_methods/object#payment_method_object-type for the list of
         * valid types. Example: `listOf("card", "klarna")`. Payment methods omitted from this list are ordered
         * by Stripe after the ones you provide; invalid payment methods are ignored.
         */
        fun paymentMethodOrder(
            paymentMethodOrder: List<String>
        ): Configuration = apply {
            this.paymentMethodOrder = paymentMethodOrder
        }

        /**
         * By default, the Payment Element will accept all card brands supported by Stripe. You can specify card
         * brands to block or allow by providing a [PaymentSheet.CardBrandAcceptance].
         *
         * **Note**: This is only a client-side solution.
         * **Note**: Card brand filtering is not currently supported in Link.
         */
        fun cardBrandAcceptance(
            cardBrandAcceptance: PaymentSheet.CardBrandAcceptance
        ): Configuration = apply {
            this.cardBrandAcceptance = cardBrandAcceptance
        }

        /**
         * By default, the Payment Element offers a card scan button within the new card entry form. When set to
         * `true`, the card entry form initializes with the card scanner already open.
         */
        fun opensCardScannerAutomatically(
            opensCardScannerAutomatically: Boolean
        ): Configuration = apply {
            this.opensCardScannerAutomatically = opensCardScannerAutomatically
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
            val appearance: PaymentSheet.Appearance,
            val preferredNetworks: List<CardBrand>,
            val paymentMethodOrder: List<String>,
            val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
            val opensCardScannerAutomatically: Boolean,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
            appearance = appearance,
            preferredNetworks = preferredNetworks,
            paymentMethodOrder = paymentMethodOrder,
            cardBrandAcceptance = cardBrandAcceptance,
            opensCardScannerAutomatically = opensCardScannerAutomatically,
        )
    }
}

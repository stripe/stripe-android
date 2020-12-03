package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.KlarnaSourceParams.LineItem
import kotlinx.parcelize.Parcelize

/**
 * Model representing parameters for creating a Klarna Source.
 *
 * See [Klarna Payments with Sources](https://stripe.com/docs/sources/klarna)
 *
 * Note:
 * The optional fields [billingEmail], [billingPhone], [billingAddress], [billingFirstName],
 * [billingLastName], and [billingDob] can be provided to skip Klarna's customer information form.
 *
 * If this information is missing, Klarna will prompt the customer for these values during checkout.
 * Be careful with this option: If the provided information is invalid,
 * Klarna may reject the transaction without giving the customer a chance to correct it.
 */
@Parcelize
data class KlarnaSourceParams @JvmOverloads constructor(
    /**
     * The URL the customer should be redirected to after they have successfully verified the
     * payment.
     */
    val purchaseCountry: String,

    /**
     * A list of [LineItem]. Klarna will present these on the confirmation page.
     * The total amount charged will be a sum of the [LineItem.totalAmount] of each of these items.
     */
    val lineItems: List<LineItem>,

    /**
     * Required for customers located in the US. This determines whether Pay Later and/or Slice It
     * is offered to a US customer.
     */
    val customPaymentMethods: Set<CustomPaymentMethods> = emptySet(),

    /**
     * An email address for the customer.
     *
     * Optional
     */
    val billingEmail: String? = null,

    /**
     * A phone number for the customer.
     *
     * Optional
     */
    val billingPhone: String? = null,

    /**
     * An [Address] for the customer. At a minimum, [Address.line1], [Address.postalCode],
     * [Address.city], and [Address.country] must be provided.
     *
     * Optional
     */
    val billingAddress: Address? = null,

    /**
     * The customer's first name.
     *
     * Optional
     */
    val billingFirstName: String? = null,

    /**
     * The customer's last name.
     *
     * Optional
     */
    val billingLastName: String? = null,

    /**
     * The customer's date of birth.
     * This will be used by Klarna for a credit check in some EU countries.
     *
     * Optional
     */
    val billingDob: DateOfBirth? = null,

    /**
     * See [Styling the Klarna Hosted Payment Page](https://stripe.com/docs/sources/klarna#styling-the-klarna-hosted-payment-page)
     *
     * You can customize the style of the Klarna hosted payment page by providing additional options
     * when creating the source. Refer to the
     * [Klarna SDK documentation](https://developers.klarna.com/en/us/kco-v3/hpp/4-customize/)
     * for more information.
     */
    val pageOptions: PaymentPageOptions? = null
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_PRODUCT to "payment",
            PARAM_PURCHASE_COUNTRY to purchaseCountry
        ).plus(
            customPaymentMethods.takeIf { it.isNotEmpty() }?.let { klarnaCustomPaymentMethods ->
                mapOf(
                    PARAM_CUSTOM_PAYMENT_METHODS to
                        klarnaCustomPaymentMethods.toList()
                            .sortedBy { it.ordinal }
                            .joinToString(",") { it.code }
                )
            }.orEmpty()
        ).plus(
            billingFirstName?.let {
                mapOf(PARAM_FIRST_NAME to it)
            }.orEmpty()
        ).plus(
            billingLastName?.let {
                mapOf(PARAM_LAST_NAME to it)
            }.orEmpty()
        ).plus(
            billingDob?.let {
                mapOf(
                    PARAM_DOB_DAY to it.day.toString().padStart(2, '0'),
                    PARAM_DOB_MONTH to it.month.toString().padStart(2, '0'),
                    PARAM_DOB_YEAR to it.year.toString()
                )
            }.orEmpty()
        )
    }

    @Parcelize
    data class LineItem @JvmOverloads constructor(
        /**
         * The line item's type. One of `sku` (for a product), `tax` (for taxes),
         * or `shipping` (for shipping costs).
         */
        val itemType: Type,

        /**
         * The human-readable description for the line item.
         */
        val itemDescription: String,

        /**
         * The total price of this line item.
         * Note: This is the total price after multiplying by the quantity,
         * not the price of an individual item.
         */
        val totalAmount: Int,

        /**
         * The quantity to display for this line item.
         */
        val quantity: Int? = null
    ) : Parcelable {
        enum class Type(internal val code: String) {
            Sku("sku"),
            Tax("tax"),
            Shipping("shipping")
        }
    }

    /**
     * Required for customers located in the US.
     */
    enum class CustomPaymentMethods(internal val code: String) {
        PayIn4("payin4"),
        Installments("installments")
    }

    /**
     * See [Styling the Klarna Hosted Payment Page](https://stripe.com/docs/sources/klarna#styling-the-klarna-hosted-payment-page)
     *
     * You can customize the style of the Klarna hosted payment page by providing additional options
     * when creating the source. Refer to the
     * [Klarna SDK documentation](https://developers.klarna.com/en/us/kco-v3/hpp/4-customize/)
     * for more information.
     */
    @Parcelize
    data class PaymentPageOptions(
        /**
         * A public URL for your businesses logo, must be served over HTTPS.
         */
        val logoUrl: String? = null,

        /**
         * A public URL for a background image, must be served over HTTPS.
         */
        val backgroundImageUrl: String? = null,

        /**
         * Title displayed on the top of the Klarna Hosted Payment Page.
         */
        val pageTitle: String? = null,

        /**
         * The buy button type.
         */
        val purchaseType: PurchaseType? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return emptyMap<String, Any>()
                .plus(
                    logoUrl?.let {
                        mapOf(PARAM_LOGO_URL to it)
                    }.orEmpty()
                )
                .plus(
                    backgroundImageUrl?.let {
                        mapOf(PARAM_BACKGROUND_IMAGE_URL to it)
                    }.orEmpty()
                )
                .plus(
                    pageTitle?.let {
                        mapOf(PARAM_PAGE_TITLE to it)
                    }.orEmpty()
                )
                .plus(
                    purchaseType?.let {
                        mapOf(PARAM_PURCHASE_TYPE to it.code)
                    }.orEmpty()
                )
        }

        /**
         * The buy button type
         */
        enum class PurchaseType(val code: String) {
            Buy("buy"),
            Rent("rent"),
            Book("book"),
            Subscribe("subscribe"),
            Download("download"),
            Order("order"),
            Continue("continue")
        }

        private companion object {
            private const val PARAM_LOGO_URL = "logo_url"
            private const val PARAM_BACKGROUND_IMAGE_URL = "background_image_url"
            private const val PARAM_PAGE_TITLE = "page_title"
            private const val PARAM_PURCHASE_TYPE = "purchase_type"
        }
    }

    private companion object {
        private const val PARAM_PURCHASE_COUNTRY = "purchase_country"
        private const val PARAM_PRODUCT = "product"
        private const val PARAM_CUSTOM_PAYMENT_METHODS = "custom_payment_methods"
        private const val PARAM_FIRST_NAME = "first_name"
        private const val PARAM_LAST_NAME = "last_name"

        private const val PARAM_DOB_DAY = "owner_dob_day"
        private const val PARAM_DOB_MONTH = "owner_dob_month"
        private const val PARAM_DOB_YEAR = "owner_dob_year"
    }
}

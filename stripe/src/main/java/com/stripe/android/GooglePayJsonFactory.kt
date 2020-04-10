package com.stripe.android

import android.content.Context
import android.os.Parcelable
import java.util.Currency
import java.util.Locale
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * A factory for generating [Google Pay JSON request objects](https://developers.google.com/pay/api/android/reference/request-objects)
 * for Google Pay API version 2.0.
 */
class GooglePayJsonFactory constructor(
    private val googlePayConfig: GooglePayConfig
) {
    /**
     * [PaymentConfiguration] must be instantiated before calling this.
     */
    constructor(context: Context) : this(GooglePayConfig(context))

    /**
     * [IsReadyToPayRequest](https://developers.google.com/pay/api/android/reference/request-objects#IsReadyToPayRequest)
     */
    fun createIsReadyToPayRequest(
        /**
         * Configure additional fields to be returned for a requested billing address.
         */
        billingAddressParameters: BillingAddressParameters? = null,

        /**
         * If set to true, then the `isReadyToPay()` class method will return `true` if the current
         * viewer is ready to pay with one or more payment methods specified in
         * `allowedPaymentMethods`.
         */
        existingPaymentMethodRequired: Boolean? = null
    ): JSONObject {
        return JSONObject()
            .put("apiVersion", API_VERSION)
            .put("apiVersionMinor", API_VERSION_MINOR)
            .put("allowedPaymentMethods",
                JSONArray()
                    .put(createCardPaymentMethod(billingAddressParameters))
            )
            .apply {
                if (existingPaymentMethodRequired != null) {
                    put("existingPaymentMethodRequired", existingPaymentMethodRequired)
                }
            }
    }

    /**
     * [PaymentDataRequest](https://developers.google.com/pay/api/android/reference/request-objects#PaymentDataRequest)
     */
    fun createPaymentDataRequest(
        /**
         * Details about the authorization of the transaction based upon whether the user agrees to
         * the transaction or not. Includes total price and price status.
         */
        transactionInfo: TransactionInfo,

        /**
         * Configure additional fields to be returned for a requested billing address.
         */
        billingAddressParameters: BillingAddressParameters? = null,

        /**
         * Specify shipping address restrictions.
         */
        shippingAddressParameters: ShippingAddressParameters? = null,

        /**
         * Set to true to request an email address.
         */
        isEmailRequired: Boolean = false,

        /**
         * Merchant name encoded as UTF-8. Merchant name is rendered in the payment sheet.
         * In TEST environment, or if a merchant isn't recognized, a “Pay Unverified Merchant” message is displayed in the payment sheet.
         */
        merchantInfo: MerchantInfo? = null
    ): JSONObject {
        return JSONObject()
            .put("apiVersion", API_VERSION)
            .put("apiVersionMinor", API_VERSION_MINOR)
            .put("allowedPaymentMethods",
                JSONArray()
                    .put(createCardPaymentMethod(billingAddressParameters))
            )
            .put("transactionInfo", createTransactionInfo(transactionInfo))
            .put("emailRequired", isEmailRequired)
            .apply {
                if (shippingAddressParameters?.isRequired == true) {
                    put("shippingAddressRequired", true)
                    put("shippingAddressParameters",
                        createShippingAddressParameters(shippingAddressParameters)
                    )
                }

                if (merchantInfo != null && !merchantInfo.merchantName.isNullOrEmpty()) {
                    put("merchantInfo", JSONObject()
                        .put("merchantName", merchantInfo.merchantName)
                    )
                }
            }
    }

    private fun createTransactionInfo(
        transactionInfo: TransactionInfo
    ): JSONObject {
        return JSONObject()
            .put("currencyCode", transactionInfo.currencyCode)
            .put("totalPriceStatus", transactionInfo.totalPriceStatus.code)
            .apply {
                transactionInfo.countryCode?.let {
                    put("countryCode", it)
                }

                transactionInfo.transactionId?.let {
                    put("transactionId", it)
                }

                transactionInfo.totalPrice?.let {
                    put("totalPrice",
                        PayWithGoogleUtils.getPriceString(
                            it, Currency.getInstance(transactionInfo.currencyCode)
                        )
                    )
                }

                transactionInfo.totalPriceLabel?.let {
                    put("totalPriceLabel", it)
                }

                transactionInfo.checkoutOption?.let {
                    put("checkoutOption", it.code)
                }
            }
    }

    private fun createShippingAddressParameters(
        shippingAddressParameters: ShippingAddressParameters
    ): JSONObject {
        return JSONObject()
            .put("allowedCountryCodes",
                JSONArray(shippingAddressParameters.allowedCountryCodes))
            .put("phoneNumberRequired",
                shippingAddressParameters.phoneNumberRequired)
    }

    private fun createCardPaymentMethod(
        billingAddressParameters: BillingAddressParameters?
    ): JSONObject {
        val cardPaymentMethodParams = createBaseCardPaymentMethodParams()
            .apply {
                if (billingAddressParameters?.isRequired == true) {
                    put("billingAddressRequired", true)
                    put("billingAddressParameters",
                        JSONObject()
                            .put("phoneNumberRequired",
                                billingAddressParameters.isPhoneNumberRequired)
                            .put("format", billingAddressParameters.format.code)
                    )
                }
            }

        return JSONObject()
            .put("type", CARD_PAYMENT_METHOD)
            .put("parameters", cardPaymentMethodParams)
            .put("tokenizationSpecification", googlePayConfig.tokenizationSpecification)
    }

    private fun createBaseCardPaymentMethodParams(): JSONObject {
        return JSONObject()
            .put("allowedAuthMethods", JSONArray(ALLOWED_AUTH_METHODS))
            .put("allowedCardNetworks", JSONArray(ALLOWED_CARD_NETWORKS))
    }

    /**
     * [BillingAddressParameters](https://developers.google.com/pay/api/android/reference/request-objects#BillingAddressParameters)
     *
     * Configure additional fields to be returned for a requested billing address.
     */
    @Parcelize
    data class BillingAddressParameters @JvmOverloads constructor(
        internal val isRequired: Boolean = false,

        /**
         * Billing address format required to complete the transaction.
         */
        internal val format: Format = Format.Min,

        /**
         * Set to true if a phone number is required to process the transaction.
         */
        internal val isPhoneNumberRequired: Boolean = false
    ) : Parcelable {
        /**
         * Billing address format required to complete the transaction.
         */
        enum class Format(internal val code: String) {
            /**
             * Name, country code, and postal code (default).
             */
            Min("MIN"),

            /**
             * Name, street address, locality, region, country code, and postal code.
             */
            Full("FULL")
        }
    }

    /**
     * [TransactionInfo](https://developers.google.com/pay/api/android/reference/request-objects#TransactionInfo)
     */
    @Parcelize
    data class TransactionInfo @JvmOverloads constructor(
        /**
         * ISO 4217 alphabetic currency code.
         */
        internal val currencyCode: String,

        /**
         * The status of the total price used.
         */
        internal val totalPriceStatus: TotalPriceStatus,

        /**
         * ISO 3166-1 alpha-2 country code where the transaction is processed.
         * This is required for merchants based in European Economic Area (EEA) countries.
         */
        internal val countryCode: String? = null,

        /**
         * A unique ID that identifies a transaction attempt. Merchants may use an existing ID or
         * generate a specific one for Google Pay transaction attempts. This field is required
         * when you send callbacks to the Google Transaction Events API.
         */
        internal val transactionId: String? = null,

        /**
         * Total monetary value of the transaction with an optional decimal precision of two
         * decimal places. This field is required unless totalPriceStatus is set to
         * NOT_CURRENTLY_KNOWN.
         *
         * The format of the string should follow the regex format: ^[0-9]+(\.[0-9][0-9])?$
         */
        internal val totalPrice: Int? = null,

        /**
         * Custom label for the total price within the display items.
         */
        internal val totalPriceLabel: String? = null,

        /**
         * Affects the submit button text displayed in the Google Pay payment sheet.
         */
        internal val checkoutOption: CheckoutOption? = null
    ) : Parcelable {
        /**
         * The status of the total price used.
         */
        enum class TotalPriceStatus(internal val code: String) {
            /**
             * Used for a capability check. Do not use this property if the transaction is
             * processed in an EEA country.
             */
            NotCurrentlyKnown("NOT_CURRENTLY_KNOWN"),

            /**
             * Total price may adjust based on the details of the response, such as sales tax
             * collected based on a billing address.
             */
            Estimated("ESTIMATED"),

            /**
             * Total price doesn't change from the amount presented to the shopper.
             */
            Final("FINAL")
        }

        /**
         * Affects the submit button text displayed in the Google Pay payment sheet.
         */
        enum class CheckoutOption(internal val code: String) {
            /**
             * Standard text applies for the given totalPriceStatus (default).
             */
            Default("DEFAULT"),

            /**
             * The selected payment method is charged immediately after the payer confirms their
             * selections. This option is only available when totalPriceStatus is set to FINAL.
             */
            CompleteImmediatePurchase("COMPLETE_IMMEDIATE_PURCHASE")
        }
    }

    /**
     * [ShippingAddressParameters](https://developers.google.com/pay/api/android/reference/request-objects#ShippingAddressParameters)
     */
    @Parcelize
    data class ShippingAddressParameters @JvmOverloads constructor(
        /**
         * Set to true to request a full shipping address.
         */
        internal val isRequired: Boolean = false,

        /**
         * ISO 3166-1 alpha-2 country code values of the countries where shipping is allowed.
         * If this object isn't specified, all shipping address countries are allowed.
         */
        internal val allowedCountryCodes: Set<String> = emptySet(),

        /**
         * Set to true if a phone number is required for the provided shipping address.
         */
        internal val phoneNumberRequired: Boolean = false
    ) : Parcelable {
        init {
            val countryCodes = Locale.getISOCountries()
            allowedCountryCodes.forEach { allowedShippingCountryCode ->
                require(
                    countryCodes.any { allowedShippingCountryCode == it }
                ) {
                    "'$allowedShippingCountryCode' is not a valid country code"
                }
            }
        }
    }

    /**
     * [MerchantInfo](https://developers.google.com/pay/api/android/reference/request-objects#MerchantInfo)
     */
    @Parcelize
    data class MerchantInfo(
        /**
         * Merchant name encoded as UTF-8. Merchant name is rendered in the payment sheet.
         * In TEST environment, or if a merchant isn't recognized, a “Pay Unverified Merchant”
         * message is displayed in the payment sheet.
         */
        internal val merchantName: String? = null
    ) : Parcelable

    companion object {
        private const val API_VERSION = 2
        private const val API_VERSION_MINOR = 0

        private const val CARD_PAYMENT_METHOD = "CARD"

        private val ALLOWED_AUTH_METHODS = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
        private val ALLOWED_CARD_NETWORKS =
            listOf("AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA")
    }
}

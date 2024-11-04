package com.stripe.android

import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.model.CardBrand
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * A factory for generating [Google Pay JSON request objects](https://developers.google.com/pay/api/android/reference/request-objects)
 * for Google Pay API version 2.0.
 */
@Singleton
class GooglePayJsonFactory constructor(
    private val googlePayConfig: GooglePayConfig,

    /**
     * Enable JCB as an allowed card network. By default, JCB is disabled.
     *
     * JCB currently can only be accepted in Japan.
     */
    private val isJcbEnabled: Boolean = false
) {
    /**
     * [PaymentConfiguration] must be instantiated before calling this.
     */
    constructor(
        context: Context,

        /**
         * Enable JCB as an allowed card network. By default, JCB is disabled.
         *
         * JCB currently can only be accepted in Japan.
         */
        isJcbEnabled: Boolean = false
    ) : this(
        googlePayConfig = GooglePayConfig(context),
        isJcbEnabled = isJcbEnabled
    )

    @Inject
    internal constructor(
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
        googlePayConfig: GooglePayPaymentMethodLauncher.Config
    ) : this(
        googlePayConfig = GooglePayConfig(publishableKeyProvider(), stripeAccountIdProvider()),
        isJcbEnabled = googlePayConfig.isJcbEnabled
    )

    /**
     * [IsReadyToPayRequest](https://developers.google.com/pay/api/android/reference/request-objects#IsReadyToPayRequest)
     */
    @JvmOverloads
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
        existingPaymentMethodRequired: Boolean? = null,

        /**
         * Set to false if you don't support credit cards
         */
        allowCreditCards: Boolean? = null
    ): JSONObject {
        return JSONObject()
            .put("apiVersion", API_VERSION)
            .put("apiVersionMinor", API_VERSION_MINOR)
            .put(
                ALLOWED_PAYMENT_METHODS,
                JSONArray()
                    .put(
                        createCardPaymentMethod(
                            billingAddressParameters,
                            allowCreditCards,
                            isReadyCall = true
                        )
                    )
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
    @JvmOverloads
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
        merchantInfo: MerchantInfo? = null,

        /**
         * Set to false if you don't support credit cards
         */
        allowCreditCards: Boolean? = null
    ): JSONObject {
        return JSONObject()
            .put("apiVersion", API_VERSION)
            .put("apiVersionMinor", API_VERSION_MINOR)
            .put(
                ALLOWED_PAYMENT_METHODS,
                JSONArray()
                    .put(
                        createCardPaymentMethod(
                            billingAddressParameters,
                            allowCreditCards
                        )
                    )
            )
            .put("transactionInfo", createTransactionInfo(transactionInfo))
            .put("emailRequired", isEmailRequired)
            .apply {
                if (shippingAddressParameters?.isRequired == true) {
                    put("shippingAddressRequired", true)
                    put(
                        "shippingAddressParameters",
                        createShippingAddressParameters(shippingAddressParameters)
                    )
                }

                if (merchantInfo != null && !merchantInfo.merchantName.isNullOrEmpty()) {
                    put(
                        "merchantInfo",
                        JSONObject()
                            .put("merchantName", merchantInfo.merchantName)
                    )
                }
            }
    }

    private fun createTransactionInfo(
        transactionInfo: TransactionInfo
    ): JSONObject {
        return JSONObject()
            .put("currencyCode", transactionInfo.currencyCode.uppercase())
            .put("totalPriceStatus", transactionInfo.totalPriceStatus.code)
            .apply {
                transactionInfo.countryCode?.let {
                    put("countryCode", it.uppercase())
                }

                transactionInfo.transactionId?.let {
                    put("transactionId", it)
                }

                transactionInfo.totalPrice?.let {
                    put(
                        "totalPrice",
                        PayWithGoogleUtils.getPriceString(
                            it,
                            Currency.getInstance(
                                transactionInfo.currencyCode.uppercase()
                            )
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
            .put(
                "allowedCountryCodes",
                JSONArray(shippingAddressParameters.normalizedAllowedCountryCodes)
            )
            .put(
                "phoneNumberRequired",
                shippingAddressParameters.phoneNumberRequired
            )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun createCardPaymentMethod(
        billingAddressParameters: BillingAddressParameters?,
        allowCreditCards: Boolean?,
        isReadyCall: Boolean = false
    ): JSONObject {
        val cardPaymentMethodParams = createBaseCardPaymentMethodParams(isReadyCall)
            .apply {
                if (billingAddressParameters?.isRequired == true) {
                    put("billingAddressRequired", true)
                    put(
                        "billingAddressParameters",
                        JSONObject()
                            .put(
                                "phoneNumberRequired",
                                billingAddressParameters.isPhoneNumberRequired
                            )
                            .put("format", billingAddressParameters.format.code)
                    )
                }
                allowCreditCards?.let {
                    put("allowCreditCards", it)
                }
            }

        return JSONObject()
            .put("type", CARD_PAYMENT_METHOD)
            .put("parameters", cardPaymentMethodParams)
            .put("tokenizationSpecification", googlePayConfig.tokenizationSpecification)
    }

    private fun createBaseCardPaymentMethodParams(isReadyCall: Boolean): JSONObject {
        val acceptedCardBrands = DEFAULT_CARD_NETWORKS
            .plus(listOf(JCB_CARD_NETWORK).takeIf { isJcbEnabled } ?: emptyList())
            .filter {
                val cardBrand = networkStringToCardBrandMap[it] ?: CardBrand.Unknown
                // Note(porter): I encountered strange behavior when using filtered card brands
                // when making the isReady call for Google Pay
                // It would error out with an unknown error, I believe this is a bug in
                // Google Pay so for the isReadyCall we don't filter card brands
                // But for subsequent calls we do, which gives the desired behavior.
                if (isReadyCall) DefaultCardBrandFilter.isAccepted(cardBrand) else cardBrandFilter.isAccepted(cardBrand)
            }
        return JSONObject()
            .put("allowedAuthMethods", JSONArray(ALLOWED_AUTH_METHODS))
            .put("allowedCardNetworks", JSONArray(acceptedCardBrands))
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

    @Parcelize
    data class TransactionInfo internal constructor(
        internal val currencyCode: String,
        internal val totalPriceStatus: TotalPriceStatus,
        internal val countryCode: String?,
        internal val transactionId: String?,
        internal val totalPrice: Long?,
        internal val totalPriceLabel: String?,
        internal val checkoutOption: CheckoutOption?,
    ) : Parcelable {

        /**
         * [TransactionInfo](https://developers.google.com/pay/api/android/reference/request-objects#TransactionInfo)
         *
         * @param currencyCode ISO 4217 alphabetic currency code.
         * @param totalPriceStatus The status of the total price used.
         * @param countryCode ISO 3166-1 alpha-2 country code where the transaction is processed. This
         * is required for merchants based in European Economic Area (EEA) countries.
         * @param transactionId A unique ID that identifies a transaction attempt. Merchants may use an
         * existing ID or generate a specific one for Google Pay transaction attempts. This field is
         * required when you send callbacks to the Google Transaction Events API.
         * @param totalPrice Total monetary value of the transaction. This field is required unless
         * [totalPriceStatus] is set to [TotalPriceStatus.NotCurrentlyKnown]. The value of this field is
         * represented in the [smallest currency unit](https://stripe.com/docs/currencies#zero-decimal).
         * For example, when [currencyCode] is `"USD"`, a value of `100` represents 100 cents ($1.00).
         * @param totalPriceLabel Custom label for the total price within the display items.
         * @param checkoutOption Affects the submit button text displayed in the Google Pay payment sheet.
         */
        @JvmOverloads
        constructor(
            currencyCode: String,
            totalPriceStatus: TotalPriceStatus,
            countryCode: String? = null,
            transactionId: String? = null,
            totalPrice: Int? = null,
            totalPriceLabel: String? = null,
            checkoutOption: CheckoutOption? = null,
        ) : this(
            currencyCode = currencyCode,
            totalPriceStatus = totalPriceStatus,
            countryCode = countryCode,
            transactionId = transactionId,
            totalPrice = totalPrice?.toLong(),
            totalPriceLabel = totalPriceLabel,
            checkoutOption = checkoutOption,
        )

        @Deprecated(
            message = "This method isn't meant for public usage and will be removed in a future release.",
        )
        fun copy(
            currencyCode: String = this.currencyCode,
            totalPriceStatus: TotalPriceStatus = this.totalPriceStatus,
            countryCode: String? = this.countryCode,
            transactionId: String? = this.transactionId,
            totalPrice: Int? = this.totalPrice?.toInt(),
            totalPriceLabel: String? = this.totalPriceLabel,
            checkoutOption: CheckoutOption? = this.checkoutOption,
        ): TransactionInfo {
            return copy(
                currencyCode = currencyCode,
                totalPriceStatus = totalPriceStatus,
                countryCode = countryCode,
                transactionId = transactionId,
                totalPrice = totalPrice?.toLong(),
                totalPriceLabel = totalPriceLabel,
                checkoutOption = checkoutOption,
            )
        }

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
        private val allowedCountryCodes: Set<String> = emptySet(),

        /**
         * Set to true if a phone number is required for the provided shipping address.
         */
        internal val phoneNumberRequired: Boolean = false
    ) : Parcelable {
        /**
         * Normalized form of [allowedCountryCodes] (i.e. capitalized country codes)
         */
        internal val normalizedAllowedCountryCodes: Set<String>
            get() {
                return allowedCountryCodes.map {
                    it.uppercase()
                }.toSet()
            }

        init {
            val countryCodes = Locale.getISOCountries()
            normalizedAllowedCountryCodes.forEach { allowedShippingCountryCode ->
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @JvmStatic
        var cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter

        private const val ALLOWED_PAYMENT_METHODS = "allowedPaymentMethods"
        private const val API_VERSION = 2
        private const val API_VERSION_MINOR = 0

        private const val CARD_PAYMENT_METHOD = "CARD"

        private val ALLOWED_AUTH_METHODS = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
        private val DEFAULT_CARD_NETWORKS =
            listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA")
        private const val JCB_CARD_NETWORK = "JCB"

        // Mapping from Google Pay string networks to CardBrands.
        private val networkStringToCardBrandMap = mapOf(
            "AMEX" to CardBrand.AmericanExpress,
            "DISCOVER" to CardBrand.Discover,
            "MASTERCARD" to CardBrand.MasterCard,
            "VISA" to CardBrand.Visa,
            "JCB" to CardBrand.JCB
        )
    }
}

package com.stripe.android.model

import androidx.annotation.IntRange
import androidx.annotation.Size
import com.stripe.android.model.Source.Companion.asSourceType
import com.stripe.android.model.Source.SourceType
import java.util.Objects
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a grouping of parameters needed to create a [Source] object on the server.
 */
class SourceParams private constructor(
    /**
     * @return a custom type of this source, if one has been set
     */
    @SourceType val typeRaw: String
) : StripeParamsModel {

    /**
     * @return the [Type][SourceType] of this source
     */
    @get:SourceType
    @SourceType
    val type: String = asSourceType(typeRaw)

    /**
     * @return the amount of the transaction
     */
    @IntRange(from = 0)
    var amount: Long? = null
        private set

    /**
     * @return a [Map] of the parameters specific to this type of source
     */
    var apiParameterMap: Map<String, Any?>? = null
        private set

    /**
     * @return the currency code for the transaction
     */
    var currency: String? = null
        private set

    /**
     * @return details about the source owner (map contents are specific to source type)
     */
    var owner: Map<String, Any>? = null
        private set

    /**
     * @return the custom metadata set on these params
     */
    var metaData: Map<String, String>? = null
        private set

    /**
     * @return redirect map for the source
     */
    var redirect: Map<String, Any?>? = null
        private set

    private var extraParams: Map<String, Any> = emptyMap()

    private var token: String? = null

    /**
     * @return the current usage of this source, if one has been set
     */
    @get:Source.Usage
    @Source.Usage
    var usage: String? = null
        private set

    private var weChatParams: WeChatParams? = null

    /*---- Setters ----*/
    /**
     * @param amount currency amount for this source, in the lowest denomination.
     * @return `this`, for chaining purposes
     */
    fun setAmount(@IntRange(from = 0) amount: Long?): SourceParams {
        this.amount = amount
        return this
    }

    /**
     * @param apiParameterMap a map of parameters specific for this type of source
     * @return `this`, for chaining purposes
     */
    fun setApiParameterMap(
        apiParameterMap: Map<String, Any?>
    ): SourceParams {
        this.apiParameterMap = apiParameterMap
        return this
    }

    /**
     * @param currency currency code for this source (i.e. "EUR")
     * @return `this`, for chaining purposes
     */
    fun setCurrency(currency: String): SourceParams {
        this.currency = currency
        return this
    }

    /**
     * @param owner an [SourceOwner] object for this source
     * @return `this`, for chaining purposes
     */
    fun setOwner(owner: Map<String, Any>): SourceParams {
        this.owner = owner.takeIf { it.isNotEmpty() }
        return this
    }

    /**
     * Sets a redirect property map for this source object. If you only want to
     * set a return url, use [setReturnUrl].
     *
     * @param redirect a set of redirect parameters
     * @return `this`, for chaining purposes
     */
    fun setRedirect(redirect: Map<String, Any?>): SourceParams {
        this.redirect = redirect
        return this
    }

    /**
     * Sets extra params for this source object.
     *
     * @param extraParams a set of params
     * @return `this`, for chaining purposes
     */
    fun setExtraParams(extraParams: Map<String, Any>): SourceParams {
        this.extraParams = extraParams
        return this
    }

    /**
     * @param returnUrl a redirect URL for this source.
     * @return `this`, for chaining purposes
     */
    fun setReturnUrl(@Size(min = 1) returnUrl: String): SourceParams {
        this.redirect = redirect.orEmpty().plus(
            mapOf(PARAM_RETURN_URL to returnUrl)
        )
        return this
    }

    /**
     * Set custom metadata on the parameters.
     *
     * @return `this`, for chaining purposes
     */
    fun setMetaData(metaData: Map<String, String>): SourceParams {
        this.metaData = metaData
        return this
    }

    /**
     * Sets a token ID on the parameters.
     *
     * @param token a token ID
     * @return `this`, for chaining purposes
     */
    fun setToken(token: String): SourceParams {
        this.token = token
        return this
    }

    /**
     * Sets a usage value on the parameters. Used for Alipay, and should be
     * either "single_use" or "reusable". Not setting this value defaults
     * to "single_use".
     *
     * @param usage either "single_use" or "reusable"
     * @return `this` for chaining purposes
     */
    fun setUsage(@Source.Usage usage: String): SourceParams {
        this.usage = usage
        return this
    }

    private fun setWeChatParams(weChatParams: WeChatParams): SourceParams {
        this.weChatParams = weChatParams
        return this
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    override fun toParamMap(): Map<String, Any> {
        return mapOf<String, Any>(PARAM_TYPE to typeRaw)
            .plus(
                apiParameterMap?.let {
                    mapOf(typeRaw to it)
                }.orEmpty()
            )
            .plus(
                amount?.let {
                    mapOf(PARAM_AMOUNT to it)
                }.orEmpty()
            )
            .plus(
                currency?.let {
                    mapOf(PARAM_CURRENCY to it)
                }.orEmpty()
            )
            .plus(
                owner.takeUnless { it.isNullOrEmpty() }?.let {
                    mapOf(PARAM_OWNER to it)
                }.orEmpty()
            )
            .plus(
                redirect?.let {
                    mapOf(PARAM_REDIRECT to it)
                }.orEmpty()
            )
            .plus(
                metaData?.let {
                    mapOf(PARAM_METADATA to it)
                }.orEmpty()
            )
            .plus(
                token?.let {
                    mapOf(PARAM_TOKEN to it)
                }.orEmpty()
            )
            .plus(
                usage?.let {
                    mapOf(PARAM_USAGE to it)
                }.orEmpty()
            )
            .plus(extraParams)
            .plus(
                weChatParams?.let {
                    mapOf(PARAM_WECHAT to it.toParamMap())
                }.orEmpty()
            )
    }

    internal data class WeChatParams(
        private val appId: String?,
        private val statementDescriptor: String?
    ) : StripeParamsModel {
        override fun toParamMap(): Map<String, Any> {
            return emptyMap<String, Any>()
                .plus(
                    appId?.let {
                        mapOf(PARAM_APPID to it)
                    }.orEmpty()
                )
                .plus(
                    statementDescriptor?.let {
                        mapOf(PARAM_STATEMENT_DESCRIPTOR to it)
                    }.orEmpty()
                )
        }

        companion object {
            private const val PARAM_APPID = "appid"
            private const val PARAM_STATEMENT_DESCRIPTOR = "statement_descriptor"
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(amount, apiParameterMap, currency, typeRaw, owner, metaData,
            redirect, extraParams, token, usage, type, weChatParams)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is SourceParams -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(params: SourceParams): Boolean {
        return Objects.equals(amount, params.amount) &&
            Objects.equals(apiParameterMap, params.apiParameterMap) &&
            Objects.equals(currency, params.currency) &&
            Objects.equals(typeRaw, params.typeRaw) &&
            Objects.equals(owner, params.owner) &&
            Objects.equals(metaData, params.metaData) &&
            Objects.equals(redirect, params.redirect) &&
            Objects.equals(extraParams, params.extraParams) &&
            Objects.equals(token, params.token) &&
            Objects.equals(usage, params.usage) &&
            Objects.equals(type, params.type) &&
            Objects.equals(weChatParams, params.weChatParams)
    }

    /**
     * [Owner param](https://stripe.com/docs/api/sources/create#create_source-owner)
     */
    private data class Owner internal constructor(
        private val address: Address? = null,
        private val email: String? = null,
        private val name: String? = null,
        private val phone: String? = null
    ) : StripeParamsModel {

        override fun toParamMap(): Map<String, Any> {
            return emptyMap<String, Any>()
                .plus(
                    address?.let {
                        mapOf(PARAM_ADDRESS to it.toParamMap())
                    }.orEmpty()
                )
                .plus(
                    email?.let {
                        mapOf(PARAM_EMAIL to it)
                    }.orEmpty()
                )
                .plus(
                    name?.let {
                        mapOf(PARAM_NAME to it)
                    }.orEmpty()
                )
                .plus(
                    phone?.let {
                        mapOf(PARAM_PHONE to it)
                    }.orEmpty()
                )
        }

        private companion object {
            private const val PARAM_ADDRESS = "address"
            private const val PARAM_EMAIL = "email"
            private const val PARAM_NAME = "name"
            private const val PARAM_PHONE = "phone"
        }
    }

    companion object {
        private const val PARAM_AMOUNT = "amount"
        private const val PARAM_CURRENCY = "currency"
        private const val PARAM_METADATA = "metadata"
        private const val PARAM_OWNER = "owner"
        private const val PARAM_REDIRECT = "redirect"
        private const val PARAM_TYPE = "type"
        private const val PARAM_TOKEN = "token"
        private const val PARAM_USAGE = "usage"
        private const val PARAM_WECHAT = "wechat"
        private const val PARAM_CLIENT_SECRET = "client_secret"
        private const val PARAM_FLOW = "flow"
        private const val PARAM_KLARNA = "klarna"
        private const val PARAM_SOURCE_ORDER = "source_order"
        private const val PARAM_BANK = "bank"
        private const val PARAM_CARD = "card"
        private const val PARAM_COUNTRY = "country"
        private const val PARAM_CVC = "cvc"
        private const val PARAM_EXP_MONTH = "exp_month"
        private const val PARAM_EXP_YEAR = "exp_year"
        private const val PARAM_IBAN = "iban"
        private const val PARAM_NUMBER = "number"
        private const val PARAM_RETURN_URL = "return_url"
        private const val PARAM_STATEMENT_DESCRIPTOR = "statement_descriptor"
        private const val PARAM_PREFERRED_LANGUAGE = "preferred_language"

        private const val PARAM_VISA_CHECKOUT = "visa_checkout"
        private const val PARAM_CALL_ID = "callid"
        private const val PARAM_MASTERPASS = "masterpass"
        private const val PARAM_TRANSACTION_ID = "transaction_id"
        private const val PARAM_CART_ID = "cart_id"

        /**
         * Create P24 Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment).
         * @param currency `eur` or `pln` (P24 payments must be in either Euros
         * or Polish Zloty).
         * @param name The name of the account holder (optional).
         * @param email The email address of the account holder.
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @return a [SourceParams] that can be used to create a P24 source
         *
         * @see [Przelewy24 Payments with Sources](https://stripe.com/docs/sources/p24)
         */
        @JvmStatic
        fun createP24Params(
            @IntRange(from = 0) amount: Long,
            currency: String,
            name: String?,
            email: String,
            returnUrl: String
        ): SourceParams {
            return SourceParams(SourceType.P24)
                .setAmount(amount)
                .setCurrency(currency)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
                .setOwner(Owner(
                    email = email,
                    name = name
                ).toParamMap())
        }

        /**
         * Create reusable Alipay Source params.
         *
         * @param currency The currency of the payment. Must be the default currency for your country.
         * Can be aud, cad, eur, gbp, hkd, jpy, nzd, sgd, or usd. Users in Denmark,
         * Norway, Sweden, or Switzerland must use eur.
         * @param name The name of the account holder (optional).
         * @param email The email address of the account holder (optional).
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @return a [SourceParams] that can be used to create an Alipay reusable source
         *
         * @see [Alipay Payments with Sources](https://stripe.com/docs/sources/alipay)
         */
        @JvmStatic
        fun createAlipayReusableParams(
            currency: String,
            name: String?,
            email: String?,
            returnUrl: String
        ): SourceParams {
            val ownerMap = Owner(
                email = email,
                name = name
            ).toParamMap()
            return SourceParams(SourceType.ALIPAY)
                .setCurrency(currency)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
                .setUsage(Source.Usage.REUSABLE)
                .setOwner(ownerMap)
        }

        /**
         * Create single-use Alipay Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a $10.99 payment).
         * @param currency The currency of the payment. Must be the default currency for your country.
         * Can be aud, cad, eur, gbp, hkd, jpy, nzd, sgd, or usd. Users in Denmark,
         * Norway, Sweden, or Switzerland must use eur.
         * @param name The name of the account holder (optional).
         * @param email The email address of the account holder (optional).
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @return a [SourceParams] that can be used to create an Alipay single-use source
         *
         * @see [Alipay Payments with Sources](https://stripe.com/docs/sources/alipay)
         */
        @JvmStatic
        fun createAlipaySingleUseParams(
            @IntRange(from = 0) amount: Long,
            currency: String,
            name: String?,
            email: String?,
            returnUrl: String
        ): SourceParams {
            val ownerMap = Owner(
                email = email,
                name = name
            ).toParamMap()
            return SourceParams(SourceType.ALIPAY)
                .setCurrency(currency)
                .setAmount(amount)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
                .setOwner(ownerMap)
        }

        /**
         * Create WeChat Pay Source params.
         *
         * @param amount A positive integer in the
         * [smallest currency unit](https://stripe.com/docs/currencies#zero-decimal)
         * representing the amount to charge the customer (e.g., 1099 for a $10.99 payment).
         * @param currency The currency of the payment. Must be the default currency for your
         * country. Can be aud, cad, eur, gbp, hkd, jpy, sgd, or usd.
         * @param weChatAppId Your registered WeChat Pay App ID from
         * [WeChat Open Platform](https://open.weixin.qq.com/).
         * @param statementDescriptor (optional) A custom statement descriptor for the payment,
         * maximum 32 characters. By default, your Stripe account’s statement descriptor is
         * used (you can review this in the [Dashboard](https://dashboard.stripe.com/account).
         */
        @JvmStatic
        fun createWeChatPayParams(
            @IntRange(from = 0) amount: Long,
            currency: String,
            weChatAppId: String,
            statementDescriptor: String?
        ): SourceParams {
            return SourceParams(SourceType.WECHAT)
                .setCurrency(currency)
                .setAmount(amount)
                .setWeChatParams(WeChatParams(weChatAppId, statementDescriptor))
        }

        /**
         * Create params for a Klarna Source
         *
         * [Klarna Payments with Sources](https://stripe.com/docs/sources/klarna)
         *
         * @param returnUrl The URL you provide to redirect the customer back to you after they
         * authenticated their payment. It can use your application URI scheme in the context of
         * a mobile application.
         * @param currency Three-letter ISO code for the currency associated with the source.
         * This is the currency for which the source will be chargeable once ready.
         * @param klarnaParams Klarna-specific params
         */
        @JvmSynthetic
        internal fun createKlarna(
            returnUrl: String,
            currency: String,
            klarnaParams: KlarnaSourceParams
        ): SourceParams {
            val totalAmount = klarnaParams.lineItems.sumBy { it.totalAmount }
            val sourceOrderParams = SourceOrderParams(
                items = klarnaParams.lineItems.map {
                    val type = when (it.itemType) {
                        KlarnaSourceParams.LineItem.Type.Sku ->
                            SourceOrderParams.Item.Type.Sku
                        KlarnaSourceParams.LineItem.Type.Tax ->
                            SourceOrderParams.Item.Type.Tax
                        KlarnaSourceParams.LineItem.Type.Shipping ->
                            SourceOrderParams.Item.Type.Shipping
                    }
                    SourceOrderParams.Item(
                        type = type,
                        amount = it.totalAmount,
                        currency = currency,
                        description = it.itemDescription,
                        quantity = it.quantity
                    )
                }
            )
            return SourceParams(SourceType.KLARNA)
                .setAmount(totalAmount.toLong())
                .setCurrency(currency)
                .setReturnUrl(returnUrl)
                .setOwner(Owner(
                    address = klarnaParams.billingAddress,
                    email = klarnaParams.billingEmail,
                    phone = klarnaParams.billingPhone
                ).toParamMap())
                .setExtraParams(
                    mapOf(
                        PARAM_KLARNA to klarnaParams.toParamMap(),
                        PARAM_FLOW to Source.SourceFlow.REDIRECT,
                        PARAM_SOURCE_ORDER to sourceOrderParams.toParamMap()
                    )
                )
        }

        /**
         * Create Bancontact Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment). The charge amount must be
         * at least €1 or its equivalent in the given currency.
         * @param name The full name of the account holder.
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @param statementDescriptor A custom statement descriptor for the payment (optional).
         * @param preferredLanguage The preferred language of the Bancontact authorization page that the
         * customer is redirected to. Supported values are: en, de, fr, or nl
         * (optional).
         * @return a [SourceParams] object that can be used to create a Bancontact source
         *
         * @see [Bancontact Payments with Sources](https://stripe.com/docs/sources/bancontact)
         */
        @JvmStatic
        fun createBancontactParams(
            @IntRange(from = 0) amount: Long,
            name: String,
            returnUrl: String,
            statementDescriptor: String?,
            preferredLanguage: String?
        ): SourceParams {
            val ownerMap = Owner(name = name)
                .toParamMap()
            val params = SourceParams(SourceType.BANCONTACT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(ownerMap)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
            val additionalParamsMap = emptyMap<String, Any>()
                .plus(
                    statementDescriptor?.let {
                        mapOf(PARAM_STATEMENT_DESCRIPTOR to it)
                    }.orEmpty()
                )
                .plus(
                    preferredLanguage?.let {
                        mapOf(PARAM_PREFERRED_LANGUAGE to it)
                    }.orEmpty()
                )
            if (additionalParamsMap.isNotEmpty()) {
                params.setApiParameterMap(additionalParamsMap)
            }
            return params
        }

        /**
         * Create a custom [SourceParams] object. Incorrect attributes may result in errors
         * when connecting to Stripe's API.
         *
         * @param type a custom type
         * @return an empty [SourceParams] object.
         */
        @JvmStatic
        fun createCustomParams(type: String): SourceParams {
            return SourceParams(type)
        }

        /**
         * Create parameters necessary for converting a token into a source
         *
         * @param tokenId the id of the [Token] to be converted into a source.
         * @return a [SourceParams] object that can be used to create a source.
         */
        @JvmStatic
        fun createSourceFromTokenParams(tokenId: String): SourceParams {
            return SourceParams(SourceType.CARD)
                .setToken(tokenId)
        }

        /**
         * Create Card Source params.
         *
         * @param card A [Card] object containing the details necessary for the source.
         * @return a [SourceParams] object that can be used to create a card source.
         *
         * @see [Card Payments with Sources](https://stripe.com/docs/sources/cards)
         */
        @JvmStatic
        fun createCardParams(card: Card): SourceParams {
            val params = SourceParams(SourceType.CARD)
            // Not enforcing all fields to exist at this level.
            // Instead, the server will return an error for invalid data.
            val cardParams = mapOf(
                PARAM_NUMBER to card.number,
                PARAM_EXP_MONTH to card.expMonth,
                PARAM_EXP_YEAR to card.expYear,
                PARAM_CVC to card.cvc
            )
            params.setApiParameterMap(cardParams)
            params.setOwner(
                Owner(
                    address = Address.Builder()
                        .setLine1(card.addressLine1)
                        .setLine2(card.addressLine2)
                        .setCity(card.addressCity)
                        .setState(card.addressState)
                        .setPostalCode(card.addressZip)
                        .setCountry(card.addressCountry)
                        .build(),
                    name = card.name
                ).toParamMap()
            )
            if (card.metadata != null) {
                params.setMetaData(card.metadata)
            }
            return params
        }

        /**
         * @param googlePayPaymentData a [JSONObject] derived from Google Pay's
         * [PaymentData#toJson()](https://developers.google.com/pay/api/android/reference/client#tojson)
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun createCardParamsFromGooglePay(
            googlePayPaymentData: JSONObject
        ): SourceParams {
            val googlePayResult = GooglePayResult.fromJson(googlePayPaymentData)
            val params = SourceParams(SourceType.CARD)
                .setToken(requireNotNull(googlePayResult.token?.id))
            return params.setOwner(
                Owner(
                    address = googlePayResult.address,
                    email = googlePayResult.email,
                    name = googlePayResult.name,
                    phone = googlePayResult.phoneNumber
                ).toParamMap()
            )
        }

        /**
         * Create EPS Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment).
         * @param name The full name of the account holder.
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @param statementDescriptor A custom statement descriptor for the payment (optional).
         * @return a [SourceParams] object that can be used to create an EPS source
         *
         * @see [EPS Payments with Sources](https://stripe.com/docs/sources/eps)
         */
        @JvmStatic
        fun createEPSParams(
            @IntRange(from = 0) amount: Long,
            name: String,
            returnUrl: String,
            statementDescriptor: String?
        ): SourceParams {
            val ownerMap = Owner(name = name)
                .toParamMap()
            val params = SourceParams(SourceType.EPS)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(ownerMap)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
            if (statementDescriptor != null) {
                params.setApiParameterMap(
                    mapOf(PARAM_STATEMENT_DESCRIPTOR to statementDescriptor)
                )
            }
            return params
        }

        /**
         * Create Giropay Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment).
         * @param name The full name of the account holder.
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @param statementDescriptor A custom statement descriptor for the payment (optional).
         * @return a [SourceParams] object that can be used to create a Giropay source
         *
         * @see [Giropay Payments with Sources](https://stripe.com/docs/sources/giropay)
         */
        @JvmStatic
        fun createGiropayParams(
            @IntRange(from = 0) amount: Long,
            name: String,
            returnUrl: String,
            statementDescriptor: String?
        ): SourceParams {
            val ownerMap = Owner(name = name)
                .toParamMap()
            val params = SourceParams(SourceType.GIROPAY)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(ownerMap)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
            if (statementDescriptor != null) {
                params.setApiParameterMap(
                    mapOf(PARAM_STATEMENT_DESCRIPTOR to statementDescriptor)
                )
            }
            return params
        }

        /**
         * Create iDEAL Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment).
         * @param name The full name of the account holder (optional).
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @param statementDescriptor A custom statement descriptor for the payment (optional).
         * @param bank The customer’s bank (optional).
         * @return a [SourceParams] object that can be used to create an iDEAL source
         *
         * @see [iDEAL Payments with Sources](https://stripe.com/docs/sources/ideal)
         */
        @JvmStatic
        fun createIdealParams(
            @IntRange(from = 0) amount: Long,
            name: String?,
            returnUrl: String,
            statementDescriptor: String?,
            bank: String?
        ): SourceParams {
            val ownerMap = Owner(name = name)
                .toParamMap()
            val params = SourceParams(SourceType.IDEAL)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
                .setOwner(ownerMap)

            val additionalParamsMap = emptyMap<String, Any>()
                .plus(
                    statementDescriptor?.let {
                        mapOf(PARAM_STATEMENT_DESCRIPTOR to it)
                    }.orEmpty()
                )
                .plus(
                    bank?.let {
                        mapOf(PARAM_BANK to it)
                    }.orEmpty()
                )
            if (additionalParamsMap.isNotEmpty()) {
                params.setApiParameterMap(additionalParamsMap)
            }

            return params
        }

        /**
         * Create Multibanco Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment).
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @param email The full email address of the customer.
         * @return a [SourceParams] object that can be used to create a Multibanco source
         *
         * @see [Multibanco Payments with Sources](https://stripe.com/docs/sources/multibanco)
         */
        @JvmStatic
        fun createMultibancoParams(
            @IntRange(from = 0) amount: Long,
            returnUrl: String,
            email: String
        ): SourceParams {
            val ownerMap = Owner(email = email)
                .toParamMap()
            return SourceParams(SourceType.MULTIBANCO)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
                .setOwner(ownerMap)
        }

        /**
         * Create SEPA Debit Source params.
         *
         * @param name The full name of the account holder.
         * @param iban The IBAN number for the bank account that you wish to debit.
         * @param addressLine1 The first line of the owner's address (optional).
         * @param city The city of the owner's address.
         * @param postalCode The postal code of the owner's address.
         * @param country The ISO-3166 2-letter country code of the owner's address.
         * @return a [SourceParams] object that can be used to create a SEPA debit source
         *
         * @see [SEPA Direct Debit Payments with Sources](https://stripe.com/docs/sources/sepa-debit)
         */
        @JvmStatic
        fun createSepaDebitParams(
            name: String,
            iban: String,
            addressLine1: String?,
            city: String,
            postalCode: String,
            @Size(2) country: String
        ): SourceParams {
            return createSepaDebitParams(name, iban, null, addressLine1, city, postalCode, country)
        }

        /**
         * Create SEPA Debit Source params.
         *
         * @param name The full name of the account holder.
         * @param iban The IBAN number for the bank account that you wish to debit.
         * @param email The full email address of the owner (optional).
         * @param addressLine1 The first line of the owner's address (optional).
         * @param city The city of the owner's address.
         * @param postalCode The postal code of the owner's address.
         * @param country The ISO-3166 2-letter country code of the owner's address.
         * @return a [SourceParams] object that can be used to create a SEPA debit source
         *
         * @see [SEPA Direct Debit Payments with Sources](https://stripe.com/docs/sources/sepa-debit)
         */
        @JvmStatic
        fun createSepaDebitParams(
            name: String,
            iban: String,
            email: String?,
            addressLine1: String?,
            city: String?,
            postalCode: String?,
            @Size(2) country: String?
        ): SourceParams {
            val ownerMap = Owner(
                address = Address.Builder()
                    .setLine1(addressLine1)
                    .setCity(city)
                    .setPostalCode(postalCode)
                    .setCountry(country)
                    .build(),
                email = email,
                name = name
            ).toParamMap()
            return SourceParams(SourceType.SEPA_DEBIT)
                .setCurrency(Source.EURO)
                .setOwner(ownerMap)
                .setApiParameterMap(mapOf(PARAM_IBAN to iban))
        }

        /**
         * Create SOFORT Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment).
         * @param returnUrl The URL the customer should be redirected to after the authorization
         * process.
         * @param country The ISO-3166 2-letter country code of the customer’s bank.
         * @param statementDescriptor A custom statement descriptor for the payment (optional).
         * @return a [SourceParams] object that can be used to create a SOFORT source
         *
         * @see [SOFORT Payments with Sources](https://stripe.com/docs/sources/sofort)
         */
        @JvmStatic
        fun createSofortParams(
            @IntRange(from = 0) amount: Long,
            returnUrl: String,
            @Size(2) country: String,
            statementDescriptor: String?
        ): SourceParams {
            val sofortMap = mapOf(PARAM_COUNTRY to country)
                .plus(
                    statementDescriptor?.let {
                        mapOf(PARAM_STATEMENT_DESCRIPTOR to it)
                    }.orEmpty()
                )
            return SourceParams(SourceType.SOFORT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
                .setApiParameterMap(sofortMap)
        }

        /**
         * Create 3D Secure Source params.
         *
         * @param amount A positive integer in the smallest currency unit representing the amount to
         * charge the customer (e.g., 1099 for a €10.99 payment).
         * @param currency The currency the payment is being created in (e.g., eur).
         * @param returnUrl The URL the customer should be redirected to after the verification process.
         * @param cardId The ID of the card source.
         * @return a [SourceParams] object that can be used to create a 3D Secure source
         *
         * @see [3D Secure Card Payments with Sources](https://stripe.com/docs/sources/three-d-secure)
         */
        @JvmStatic
        fun createThreeDSecureParams(
            @IntRange(from = 0) amount: Long,
            currency: String,
            returnUrl: String,
            cardId: String
        ): SourceParams {
            return SourceParams(SourceType.THREE_D_SECURE)
                .setCurrency(currency)
                .setAmount(amount)
                .setRedirect(mapOf(PARAM_RETURN_URL to returnUrl))
                .setApiParameterMap(mapOf(PARAM_CARD to cardId))
        }

        /**
         * Create parameters needed to make a Visa Checkout source.
         *
         * @param callId The payment request ID (callId) from the Visa Checkout SDK.
         * @return a [SourceParams] object that can be used to create a Visa Checkout Card Source.
         *
         * @see [https://stripe.com/docs/visa-checkout](https://stripe.com/docs/visa-checkout)
         *
         * @see [https://developer.visa.com/capabilities/visa_checkout/docs](https://developer.visa.com/capabilities/visa_checkout/docs)
         */
        @JvmStatic
        fun createVisaCheckoutParams(callId: String): SourceParams {
            return SourceParams(SourceType.CARD)
                .setApiParameterMap(
                    mapOf(PARAM_VISA_CHECKOUT to mapOf(PARAM_CALL_ID to callId)))
        }

        /**
         * Create parameters needed to make a Masterpass source
         *
         * @param transactionId The transaction ID from the Masterpass SDK.
         * @param cartId A unique string that you generate to identify the purchase when creating a cart
         * for checkout in the Masterpass SDK.
         *
         * @return a [SourceParams] object that can be used to create a Masterpass Card Source.
         *
         * @see [https://stripe.com/docs/masterpass](https://stripe.com/docs/masterpass)
         *
         * @see [https://developer.mastercard.com/product/masterpass](https://developer.mastercard.com/product/masterpass)
         *
         * @see [https://developer.mastercard.com/page/masterpass-merchant-mobile-checkout-sdk-for-android-v2](https://developer.mastercard.com/page/masterpass-merchant-mobile-checkout-sdk-for-android-v2)
         */
        @JvmStatic
        fun createMasterpassParams(
            transactionId: String,
            cartId: String
        ): SourceParams {
            val map = mapOf(
                PARAM_TRANSACTION_ID to transactionId,
                PARAM_CART_ID to cartId
            )
            return SourceParams(SourceType.CARD)
                .setApiParameterMap(mapOf(PARAM_MASTERPASS to map))
        }

        /**
         * Create parameters needed to retrieve a source.
         *
         * @param clientSecret the client secret for the source, needed because the Android SDK uses
         * a public key
         * @return a [Map] matching the parameter name to the client secret, ready to send to
         * the server.
         */
        @JvmStatic
        fun createRetrieveSourceParams(
            @Size(min = 1) clientSecret: String
        ): Map<String, String> {
            return mapOf(PARAM_CLIENT_SECRET to clientSecret)
        }
    }
}

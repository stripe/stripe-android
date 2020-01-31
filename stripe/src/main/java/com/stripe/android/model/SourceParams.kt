package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.IntRange
import androidx.annotation.Size
import com.stripe.android.model.Source.Companion.asSourceType
import com.stripe.android.model.Source.SourceType
import java.util.Objects
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a grouping of parameters needed to create a [Source] object on the server.
 */
class SourceParams private constructor(
    /**
     * @return a custom type of this Source, if one has been set
     */
    @SourceType val typeRaw: String,

    internal val attribution: Set<String> = emptySet()
) : StripeParamsModel {

    /**
     * @return The type of the source to create.
     */
    @get:SourceType
    @SourceType
    val type: String = asSourceType(typeRaw)

    /**
     * @return Amount associated with the source. This is the amount for which the source will
     * be chargeable once ready. Required for `single_use` sources. Not supported for `receiver`
     * type sources, where charge amount may not be specified until funds land.
     *
     * See [amount](https://stripe.com/docs/api/sources/create#create_source-amount)
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
     * @return Three-letter ISO code for the currency associated with the source.
     * This is the currency for which the source will be chargeable once ready.
     *
     * See [currency](https://stripe.com/docs/api/sources/create#create_source-currency)
     */
    var currency: String? = null
        private set

    /**
     * The URL you provide to redirect the customer back to you after they authenticated their
     * payment. It can use your application URI scheme in the context of a mobile application.
     */
    var returnUrl: String? = null
        private set

    /**
     * Information about the owner of the payment instrument that may be used or required by
     * particular source types.
     */
    var owner: OwnerParams? = null
        private set

    /**
     * @return the custom metadata set on these params
     */
    var metaData: Map<String, String>? = null
        private set

    private var extraParams: Map<String, Any> = emptyMap()

    /**
     * @return An optional token used to create the source. When passed, token properties will
     * override source parameters.
     *
     * See [token](https://stripe.com/docs/api/sources/create#create_source-token)
     */
    private var token: String? = null

    /**
     * @return Either `reusable` or `single_use`. Whether this source should be reusable or not.
     * Some source types may or may not be reusable by construction, while others may leave the
     * option at creation. If an incompatible value is passed, an error will be returned.
     *
     * See [usage](https://stripe.com/docs/api/sources/create#create_source-usage)
     */
    @get:Source.Usage
    @Source.Usage
    var usage: String? = null
        private set

    private var weChatParams: WeChatParams? = null

    /*---- Setters ----*/
    /**
     * @param amount Amount associated with the source. This is the amount for which the source will
     * be chargeable once ready. Required for `single_use` sources. Not supported for `receiver`
     * type sources, where charge amount may not be specified until funds land.
     *
     * See [amount](https://stripe.com/docs/api/sources/create#create_source-amount)
     */
    fun setAmount(@IntRange(from = 0) amount: Long?): SourceParams = apply {
        this.amount = amount
    }

    /**
     * @param apiParameterMap a map of parameters specific for this type of source
     */
    fun setApiParameterMap(apiParameterMap: Map<String, Any?>?): SourceParams = apply {
        this.apiParameterMap = apiParameterMap
    }

    /**
     * @param currency Three-letter ISO code for the currency associated with the source.
     * This is the currency for which the source will be chargeable once ready.
     *
     * See [currency](https://stripe.com/docs/api/sources/create#create_source-currency)
     */
    fun setCurrency(currency: String): SourceParams = apply {
        this.currency = currency
    }

    /**
     * @param owner Information about the owner of the payment instrument that may be used or
     * required by particular source types.
     *
     * See [owner](https://stripe.com/docs/api/sources/create#create_source-owner)
     */
    fun setOwner(owner: OwnerParams?): SourceParams = apply {
        this.owner = owner
    }

    /**
     * Sets extra params for this source object.
     *
     * @param extraParams a set of params
     */
    fun setExtraParams(extraParams: Map<String, Any>): SourceParams = apply {
        this.extraParams = extraParams
    }

    /**
     * @param returnUrl The URL you provide to redirect the customer back to you after they
     * authenticated their payment. It can use your application URI scheme in the context of a
     * mobile application.
     *
     * See [redirect.return_url](https://stripe.com/docs/api/sources/create#create_source-redirect-return_url)
     */
    fun setReturnUrl(@Size(min = 1) returnUrl: String): SourceParams = apply {
        this.returnUrl = returnUrl
    }

    /**
     * @param metaData A set of key-value pairs that you can attach to a source object. It can be
     * useful for storing additional information about the source in a structured format.
     *
     * See [metadata](https://stripe.com/docs/api/sources/create#create_source-metadata)
     */
    fun setMetaData(metaData: Map<String, String>?): SourceParams = apply {
        this.metaData = metaData
    }

    /**
     * @param token An optional token used to create the source. When passed, token properties will
     * override source parameters.
     *
     * See [token](https://stripe.com/docs/api/sources/create#create_source-token)
     */
    fun setToken(token: String): SourceParams = apply {
        this.token = token
    }

    /**
     * @param usage Either `reusable` or `single_use`. Whether this source should be reusable or not.
     * Some source types may or may not be reusable by construction, while others may leave the
     * option at creation. If an incompatible value is passed, an error will be returned.
     *
     * See [usage](https://stripe.com/docs/api/sources/create#create_source-usage)
     */
    fun setUsage(@Source.Usage usage: String): SourceParams = apply {
        this.usage = usage
    }

    private fun setWeChatParams(weChatParams: WeChatParams): SourceParams {
        this.weChatParams = weChatParams
        return this
    }

    /**
     * Create a string-keyed map representing this object that is ready to be sent over the network.
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
                owner?.let {
                    mapOf(PARAM_OWNER to it.toParamMap())
                }.orEmpty()
            )
            .plus(
                returnUrl?.let {
                    mapOf(PARAM_REDIRECT to mapOf(PARAM_RETURN_URL to it))
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

    @Parcelize
    internal data class WeChatParams(
        private val appId: String? = null,
        private val statementDescriptor: String? = null
    ) : StripeParamsModel, Parcelable {
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
            returnUrl, extraParams, token, usage, type, weChatParams)
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
            Objects.equals(returnUrl, params.returnUrl) &&
            Objects.equals(extraParams, params.extraParams) &&
            Objects.equals(token, params.token) &&
            Objects.equals(usage, params.usage) &&
            Objects.equals(type, params.type) &&
            Objects.equals(weChatParams, params.weChatParams)
    }

    /**
     * [owner](https://stripe.com/docs/api/sources/create#create_source-owner) param
     *
     * Information about the owner of the payment instrument that may be used or required by
     * particular source types.
     */
    @Parcelize
    data class OwnerParams internal constructor(
        internal val address: Address? = null,
        internal val email: String? = null,
        internal val name: String? = null,
        internal val phone: String? = null
    ) : StripeParamsModel, Parcelable {
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
                .setReturnUrl(returnUrl)
                .setOwner(
                    OwnerParams(
                        email = email,
                        name = name
                    )
                )
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
            name: String? = null,
            email: String? = null,
            returnUrl: String
        ): SourceParams {
            return SourceParams(SourceType.ALIPAY)
                .setCurrency(currency)
                .setReturnUrl(returnUrl)
                .setUsage(Source.Usage.REUSABLE)
                .setOwner(
                    OwnerParams(
                        email = email,
                        name = name
                    )
                )
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
            name: String? = null,
            email: String? = null,
            returnUrl: String
        ): SourceParams {
            return SourceParams(SourceType.ALIPAY)
                .setCurrency(currency)
                .setAmount(amount)
                .setReturnUrl(returnUrl)
                .setOwner(
                    OwnerParams(
                        email = email,
                        name = name
                    )
                )
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
            statementDescriptor: String? = null
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
        @JvmStatic
        fun createKlarna(
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
                .setOwner(
                    OwnerParams(
                        address = klarnaParams.billingAddress,
                        email = klarnaParams.billingEmail,
                        phone = klarnaParams.billingPhone
                    )
                )
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
            statementDescriptor: String? = null,
            preferredLanguage: String? = null
        ): SourceParams {
            val params = SourceParams(SourceType.BANCONTACT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(
                    OwnerParams(name = name)
                )
                .setReturnUrl(returnUrl)
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
            return SourceParams(SourceType.CARD, card.loggingTokens)
                .setApiParameterMap(
                    mapOf(
                        PARAM_NUMBER to card.number,
                        PARAM_EXP_MONTH to card.expMonth,
                        PARAM_EXP_YEAR to card.expYear,
                        PARAM_CVC to card.cvc
                    )
                )
                .setOwner(
                    OwnerParams(
                        address = Address.Builder()
                            .setLine1(card.addressLine1)
                            .setLine2(card.addressLine2)
                            .setCity(card.addressCity)
                            .setState(card.addressState)
                            .setPostalCode(card.addressZip)
                            .setCountry(card.addressCountry)
                            .build(),
                        name = card.name
                    )
                )
                .setMetaData(card.metadata)
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
            return SourceParams(SourceType.CARD)
                .setToken(requireNotNull(googlePayResult.token?.id))
                .setOwner(
                    OwnerParams(
                        address = googlePayResult.address,
                        email = googlePayResult.email,
                        name = googlePayResult.name,
                        phone = googlePayResult.phoneNumber
                    )
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
            statementDescriptor: String? = null
        ): SourceParams {
            return SourceParams(SourceType.EPS)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(OwnerParams(name = name))
                .setReturnUrl(returnUrl)
                .setApiParameterMap(
                    statementDescriptor?.let {
                        mapOf(PARAM_STATEMENT_DESCRIPTOR to it)
                    }
                )
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
            statementDescriptor: String? = null
        ): SourceParams {
            return SourceParams(SourceType.GIROPAY)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(OwnerParams(name = name))
                .setReturnUrl(returnUrl)
                .setApiParameterMap(
                    statementDescriptor?.let {
                        mapOf(PARAM_STATEMENT_DESCRIPTOR to it)
                    }
                )
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
            statementDescriptor: String? = null,
            bank: String? = null
        ): SourceParams {
            val params = SourceParams(SourceType.IDEAL)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setReturnUrl(returnUrl)
                .setOwner(OwnerParams(name = name))

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
            return SourceParams(SourceType.MULTIBANCO)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setReturnUrl(returnUrl)
                .setOwner(OwnerParams(email = email))
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
            return SourceParams(SourceType.SEPA_DEBIT)
                .setCurrency(Source.EURO)
                .setOwner(
                    OwnerParams(
                        address = Address.Builder()
                            .setLine1(addressLine1)
                            .setCity(city)
                            .setPostalCode(postalCode)
                            .setCountry(country)
                            .build(),
                        email = email,
                        name = name
                    )
                )
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
            statementDescriptor: String? = null
        ): SourceParams {
            val sofortData = mapOf(PARAM_COUNTRY to country)
                .plus(
                    statementDescriptor?.let {
                        mapOf(PARAM_STATEMENT_DESCRIPTOR to it)
                    }.orEmpty()
                )
            return SourceParams(SourceType.SOFORT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setReturnUrl(returnUrl)
                .setApiParameterMap(sofortData)
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
                .setReturnUrl(returnUrl)
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

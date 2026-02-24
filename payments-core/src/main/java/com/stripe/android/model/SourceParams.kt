package com.stripe.android.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntRange
import androidx.annotation.Size
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.model.Source.Companion.asSourceType
import com.stripe.android.model.Source.SourceType
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a grouping of parameters needed to create a [Source] object on the server.
 */
@Parcelize
@Poko
class SourceParams internal constructor(
    /**
     * The type of the source to create.
     */
    @SourceType val typeRaw: String,
    internal var typeData: TypeData? = null,
    /**
     * Amount associated with the source. This is the amount for which the source will
     * be chargeable once ready. Required for `single_use` sources. Not supported for `receiver`
     * type sources, where charge amount may not be specified until funds land.
     *
     * See [amount](https://stripe.com/docs/api/sources/create#create_source-amount)
     */
    var amount: Long? = null,
    /**
     * Three-letter ISO code for the currency associated with the source.
     * This is the currency for which the source will be chargeable once ready.
     *
     * See [currency](https://stripe.com/docs/api/sources/create#create_source-currency)
     */
    var currency: String? = null,
    /**
     * Information about the owner of the payment instrument that may be used or required by
     * particular source types.
     */
    var owner: OwnerParams? = null,
    /**
     * Either `reusable` or `single_use`. Whether this source should be reusable or not.
     * Some source types may or may not be reusable by construction, while others may leave the
     * option at creation. If an incompatible value is passed, an error will be returned.
     *
     * See [usage](https://stripe.com/docs/api/sources/create#create_source-usage)
     */
    var usage: Source.Usage? = null,
    /**
     * Information about the items and shipping associated with the source. Required for
     * transactional credit (for example Klarna) sources before you can charge it.
     *
     * See [source_order](https://stripe.com/docs/api/sources/create#create_source-source_order)
     */
    var sourceOrder: SourceOrderParams? = null,
    /**
     * An optional token used to create the source. When passed, token properties will
     * override source parameters.
     *
     * See [token](https://stripe.com/docs/api/sources/create#create_source-token)
     */
    var token: String? = null,
    /**
     * Set of key-value pairs that you can attach to an object. This can be useful for storing
     * additional information about the object in a structured format.
     */
    var metadata: Map<String, String>? = null,
    private var apiParams: ApiParams = ApiParams(),
    /**
     * A set of identifiers representing the component that created this instance.
     */
    internal val attribution: Set<String> = emptySet()
) : StripeParamsModel, Parcelable {

    /**
     * The type of the source to create.
     */
    @get:SourceType
    @SourceType
    val type: String
        get() = asSourceType(typeRaw)

    /**
     * A [Map] of the parameters specific to the Source type.
     */
    val apiParameterMap: Map<String, Any?> get() = apiParams.value

    /*---- Setters ----*/

    /**
     * @param apiParameterMap a map of parameters specific for this type of source
     */
    fun setApiParameterMap(
        apiParameterMap: Map<String, Any?>?
    ): SourceParams = apply {
        this.apiParams = ApiParams(apiParameterMap.orEmpty())
    }

    /**
     * Create a string-keyed map representing this object that is ready to be sent over the network.
     */
    @Suppress("LongMethod")
    override fun toParamMap(): Map<String, Any> {
        return mapOf<String, Any>(PARAM_TYPE to typeRaw)
            .plus(
                apiParams.value.takeIf {
                    it.isNotEmpty()
                }?.let {
                    mapOf(typeRaw to it)
                }.orEmpty()
            )
            .plus(
                typeData?.createParams().orEmpty()
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
                sourceOrder?.let {
                    mapOf(PARAM_SOURCE_ORDER to it.toParamMap())
                }.orEmpty()
            )
            .plus(
                owner?.let {
                    mapOf(PARAM_OWNER to it.toParamMap())
                }.orEmpty()
            )
            .plus(
                metadata?.let {
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
                    mapOf(PARAM_USAGE to it.code)
                }.orEmpty()
            )
    }

    /**
     * Information about the owner of the payment instrument that may be used or required by
     * particular source types.
     *
     * See [owner](https://stripe.com/docs/api/sources/create#create_source-owner).
     */
    @Parcelize
    @Poko
    class OwnerParams @JvmOverloads constructor(
        internal var address: Address? = null,
        internal var email: String? = null,
        internal var name: String? = null,
        internal var phone: String? = null
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
        private const val PARAM_CLIENT_SECRET = "client_secret"
        private const val PARAM_CURRENCY = "currency"
        private const val PARAM_METADATA = "metadata"
        private const val PARAM_OWNER = "owner"
        private const val PARAM_SOURCE_ORDER = "source_order"
        private const val PARAM_TOKEN = "token"
        private const val PARAM_TYPE = "type"
        private const val PARAM_USAGE = "usage"

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
            return SourceParams(
                SourceType.CARD,
                token = tokenId
            )
        }

        /**
         * Create Card Source params.
         *
         * @param cardParams A [CardParams] object containing the details necessary for the source.
         * @return a [SourceParams] object that can be used to create a card source.
         *
         * @see [Card Payments with Sources](https://stripe.com/docs/sources/cards)
         */
        @JvmStatic
        fun createCardParams(
            cardParams: CardParams
        ): SourceParams {
            @OptIn(DelicateCardDetailsApi::class)
            return SourceParams(
                SourceType.CARD,
                typeData = TypeData.Card(
                    cardParams.number,
                    cardParams.expMonth,
                    cardParams.expYear,
                    cardParams.cvc
                ),
                attribution = cardParams.attribution,
                owner = OwnerParams(
                    address = cardParams.address,
                    name = cardParams.name
                ),
                metadata = cardParams.metadata
            )
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
            val token = googlePayResult.token

            return SourceParams(
                SourceType.CARD,
                token = token?.id.orEmpty(),
                attribution = setOfNotNull(
                    token?.card?.tokenizationMethod?.toString()
                ),
                owner = OwnerParams(
                    address = googlePayResult.address,
                    email = googlePayResult.email,
                    name = googlePayResult.name,
                    phone = googlePayResult.phoneNumber
                )
            )
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

    @Parcelize
    @Poko
    internal class ApiParams(
        val value: Map<String, Any?> = emptyMap()
    ) : Parcelable {
        internal companion object : Parceler<ApiParams> {
            override fun ApiParams.write(parcel: Parcel, flags: Int) {
                parcel.writeString(
                    StripeJsonUtils.mapToJsonObject(value)?.toString()
                )
            }

            override fun create(parcel: Parcel): ApiParams {
                return ApiParams(
                    StripeJsonUtils.jsonObjectToMap(
                        parcel.readString()?.let {
                            JSONObject(it)
                        }
                    ).orEmpty()
                )
            }
        }
    }

    internal sealed class TypeData : Parcelable {
        abstract val type: String

        abstract val params: List<Pair<String, Any?>>

        fun createParams(): Map<String, Map<String, Any>> {
            return params.fold(
                emptyMap<String, Any>()
            ) { acc, (key, value) ->
                acc.plus(
                    value?.let { mapOf(key to it) }.orEmpty()
                )
            }.takeIf { it.isNotEmpty() }?.let {
                mapOf(
                    type to it
                )
            }.orEmpty()
        }

        @Parcelize
        @Poko
        class Card(
            /**
             * The [number] of this card
             */
            val number: String? = null,
            /**
             * Two-digit number representing the card’s expiration month.
             *
             * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-exp_month).
             */
            @get:IntRange(from = 1, to = 12)
            val expMonth: Int?,
            /**
             * Four-digit number representing the card’s expiration year.
             *
             * See [API Reference](https://stripe.com/docs/api/cards/object#card_object-exp_year).
             */
            val expYear: Int?,
            /**
             * The [cvc] for this card
             */
            val cvc: String? = null
        ) : TypeData() {
            override val type: String get() = SourceType.CARD

            override val params: List<Pair<String, Any?>>
                get() = listOf(
                    PARAM_NUMBER to number,
                    PARAM_EXP_MONTH to expMonth,
                    PARAM_EXP_YEAR to expYear,
                    PARAM_CVC to cvc
                )

            private companion object {
                private const val PARAM_NUMBER = "number"
                private const val PARAM_EXP_MONTH = "exp_month"
                private const val PARAM_EXP_YEAR = "exp_year"
                private const val PARAM_CVC = "cvc"
            }
        }
    }
}

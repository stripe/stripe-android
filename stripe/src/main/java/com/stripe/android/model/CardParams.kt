package com.stripe.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * [Create a card token](https://stripe.com/docs/api/tokens/create_card)
 */
@Parcelize
internal data class CardParams @JvmOverloads constructor(
    /**
     * [card.number](https://stripe.com/docs/api/tokens/create_card#create_card_token-card-number)
     *
     * Required
     *
     * The card number, as a string without any separators.
     */
    private val number: String,

    /**
     * [card.exp_month](https://stripe.com/docs/api/tokens/create_card#create_card_token-card-exp_month)
     *
     * Required
     *
     * Two-digit number representing the card's expiration month.
     */
    private val expMonth: Int,

    /**
     * [card.exp_year](https://stripe.com/docs/api/tokens/create_card#create_card_token-card-exp_year)
     *
     * Required
     *
     * Two- or four-digit number representing the card's expiration year.
     */
    private val expYear: Int,

    /**
     * [card.cvc](https://stripe.com/docs/api/tokens/create_card#create_card_token-card-cvc)
     *
     * Usually required
     *
     * Card security code. Highly recommended to always include this value, but it's required only
     * for accounts based in European countries.
     */
    private val cvc: String? = null,

    /**
     * [card.name](https://stripe.com/docs/api/tokens/create_card#create_card_token-card-name)
     *
     * Optional
     *
     * Cardholder's full name.
     */
    private val name: String? = null,

    private val address: Address? = null,

    /**
     * [card.currency](https://stripe.com/docs/api/tokens/create_card#create_card_token-card-currency)
     *
     * Optional - Custom Connect Only
     *
     * Required in order to add the card to an account; in all other cases, this parameter is
     * not used. When added to an account, the card (which must be a debit card) can be used
     * as a transfer destination for funds in this currency. Currently, the only supported
     * currency for debit card payouts is `usd`.
     */
    private val currency: String? = null
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        val params: Map<String, Any> = listOf(
            PARAM_NUMBER to number,
            PARAM_EXP_MONTH to expMonth,
            PARAM_EXP_YEAR to expYear,
            PARAM_CVC to cvc,
            PARAM_NAME to name,
            PARAM_CURRENCY to currency,
            PARAM_ADDRESS_LINE1 to address?.line1,
            PARAM_ADDRESS_LINE2 to address?.line2,
            PARAM_ADDRESS_CITY to address?.city,
            PARAM_ADDRESS_STATE to address?.state,
            PARAM_ADDRESS_ZIP to address?.postalCode,
            PARAM_ADDRESS_COUNTRY to address?.country
        ).fold(emptyMap()) { acc, (key, value) ->
            acc.plus(
                value?.let { mapOf(key to it) }.orEmpty()
            )
        }

        return mapOf(Token.TokenType.CARD to params)
    }

    private companion object {
        private const val PARAM_NUMBER = "number"
        private const val PARAM_EXP_MONTH = "exp_month"
        private const val PARAM_EXP_YEAR = "exp_year"
        private const val PARAM_CVC = "cvc"
        private const val PARAM_NAME = "name"
        private const val PARAM_ADDRESS_LINE1 = "address_line1"
        private const val PARAM_ADDRESS_LINE2 = "address_line2"
        private const val PARAM_ADDRESS_CITY = "address_city"
        private const val PARAM_ADDRESS_STATE = "address_state"
        private const val PARAM_ADDRESS_ZIP = "address_zip"
        private const val PARAM_ADDRESS_COUNTRY = "address_country"
        private const val PARAM_CURRENCY = "currency"
    }
}

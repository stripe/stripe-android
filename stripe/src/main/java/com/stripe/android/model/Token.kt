package com.stripe.android.model

import androidx.annotation.StringDef
import com.stripe.android.model.Token.TokenType
import java.util.Date
import org.json.JSONException
import org.json.JSONObject

/**
 * Tokenization is the process Stripe uses to collect sensitive card, bank account details, Stripe
 * account details or personally identifiable information (PII), directly from your customers in a
 * secure manner. A Token representing this information is returned to you to use.
 */
data class Token private constructor(

    /**
     * @return the Token id
     */
    override val id: String,

    /**
     * @return Get the [TokenType] of this token.
     */
    @get:TokenType
    val type: String,

    /***
     * @return the [Date] this token was created
     */
    val created: Date,

    /**
     * @return `true` if this token is valid for a real payment, `false` if
     * it is only usable for testing
     */
    val livemode: Boolean,

    /**
     * @return `true` if this token has been used, `false` otherwise
     */
    val used: Boolean,

    /**
     * @return the [BankAccount] for this token
     */
    val bankAccount: BankAccount? = null,

    /**
     * @return the [Card] for this token
     */
    val card: Card? = null
) : StripePaymentSource {
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(TokenType.CARD, TokenType.BANK_ACCOUNT, TokenType.PII, TokenType.ACCOUNT,
        TokenType.CVC_UPDATE)
    annotation class TokenType {
        companion object {
            const val CARD: String = "card"
            const val BANK_ACCOUNT: String = "bank_account"
            const val PII: String = "pii"
            const val ACCOUNT: String = "account"
            const val CVC_UPDATE: String = "cvc_update"
        }
    }

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    constructor(
        id: String,
        livemode: Boolean,
        created: Date,
        used: Boolean?,
        card: Card?
    ) : this(
        id = id,
        type = TokenType.CARD,
        created = created,
        livemode = livemode,
        card = card,
        used = java.lang.Boolean.TRUE == used
    )

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    constructor(
        id: String,
        livemode: Boolean,
        created: Date,
        used: Boolean?,
        bankAccount: BankAccount
    ) : this(
        id = id,
        type = TokenType.BANK_ACCOUNT,
        created = created,
        livemode = livemode,
        used = java.lang.Boolean.TRUE == used,
        bankAccount = bankAccount
    )

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    constructor(
        id: String,
        type: String,
        livemode: Boolean,
        created: Date,
        used: Boolean?
    ) : this(
        id = id,
        type = type,
        created = created,
        used = java.lang.Boolean.TRUE == used,
        livemode = livemode
    )

    companion object {
        // The key for these object fields is identical to their retrieved values
        // from the Type field.
        private const val FIELD_BANK_ACCOUNT = TokenType.BANK_ACCOUNT
        private const val FIELD_CARD = TokenType.CARD
        private const val FIELD_CREATED = "created"
        private const val FIELD_ID = "id"
        private const val FIELD_LIVEMODE = "livemode"

        private const val FIELD_TYPE = "type"
        private const val FIELD_USED = "used"

        @JvmStatic
        fun fromString(jsonString: String?): Token? {
            if (jsonString == null) {
                return null
            }
            return try {
                val tokenObject = JSONObject(jsonString)
                fromJson(tokenObject)
            } catch (exception: JSONException) {
                null
            }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Token? {
            if (jsonObject == null) {
                return null
            }
            val tokenId = StripeJsonUtils.optString(jsonObject, FIELD_ID)
            val createdTimeStamp = StripeJsonUtils.optLong(jsonObject, FIELD_CREATED)
            @TokenType val tokenType =
                asTokenType(StripeJsonUtils.optString(jsonObject, FIELD_TYPE))
            if (tokenId == null || createdTimeStamp == null) {
                return null
            }

            val used = StripeJsonUtils.optBoolean(jsonObject, FIELD_USED)
            val liveMode = StripeJsonUtils.optBoolean(jsonObject, FIELD_LIVEMODE)
            val date = Date(createdTimeStamp * 1000)

            return if (TokenType.BANK_ACCOUNT == tokenType) {
                val bankAccountObject = jsonObject.optJSONObject(FIELD_BANK_ACCOUNT) ?: return null
                Token(tokenId, liveMode, date, used, BankAccount.fromJson(bankAccountObject))
            } else if (TokenType.CARD == tokenType) {
                val cardObject = jsonObject.optJSONObject(FIELD_CARD) ?: return null
                Token(tokenId, liveMode, date, used, Card.fromJson(cardObject))
            } else if (TokenType.PII == tokenType || TokenType.ACCOUNT == tokenType ||
                TokenType.CVC_UPDATE == tokenType) {
                Token(tokenId, tokenType, liveMode, date, used)
            } else {
                null
            }
        }

        /**
         * Converts an unchecked String value to a [TokenType] or `null`.
         *
         * @param possibleTokenType a String that might match a [TokenType] or be empty
         * @return `null` if the input is blank or otherwise does not match a [TokenType],
         * else the appropriate [TokenType].
         */
        @TokenType
        private fun asTokenType(possibleTokenType: String?): String? {
            return when (possibleTokenType) {
                TokenType.CARD -> TokenType.CARD
                TokenType.BANK_ACCOUNT -> TokenType.BANK_ACCOUNT
                TokenType.PII -> TokenType.PII
                TokenType.ACCOUNT -> TokenType.ACCOUNT
                TokenType.CVC_UPDATE -> TokenType.CVC_UPDATE
                else -> null
            }
        }
    }
}

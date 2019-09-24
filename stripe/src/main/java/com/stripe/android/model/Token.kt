package com.stripe.android.model

import androidx.annotation.StringDef
import com.stripe.android.StripeTextUtils
import java.util.Date
import java.util.Objects
import org.json.JSONException
import org.json.JSONObject

/**
 * Tokenization is the process Stripe uses to collect sensitive card, bank account details, Stripe
 * account details or personally identifiable information (PII), directly from your customers in a
 * secure manner. A Token representing this information is returned to you to use.
 */
class Token : StripePaymentSource {

    /**
     * @return the Token id
     */
    override val id: String

    /**
     * @return Get the [TokenType] of this token.
     */
    @get:TokenType
    val type: String

    /***
     * @return the [Date] this token was created
     */
    val created: Date
    /**
     * @return `true` if this token is valid for a real payment, `false` if
     * it is only usable for testing
     */
    val livemode: Boolean

    /**
     * @return `true` if this token has been used, `false` otherwise
     */
    val used: Boolean

    /**
     * @return the [BankAccount] for this token
     */
    val bankAccount: BankAccount?

    /**
     * @return the [Card] for this token
     */
    val card: Card?

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(TokenType.CARD, TokenType.BANK_ACCOUNT, TokenType.PII, TokenType.ACCOUNT,
        TokenType.CVC_UPDATE)
    annotation class TokenType {
        companion object {
            const val CARD = "card"
            const val BANK_ACCOUNT = "bank_account"
            const val PII = "pii"
            const val ACCOUNT = "account"
            const val CVC_UPDATE = "cvc_update"
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
    ) {
        this.id = id
        type = TokenType.CARD
        this.created = created
        this.livemode = livemode
        this.card = card
        this.used = java.lang.Boolean.TRUE == used
        bankAccount = null
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
        bankAccount: BankAccount
    ) {
        this.id = id
        type = TokenType.BANK_ACCOUNT
        this.created = created
        this.livemode = livemode
        card = null
        this.used = java.lang.Boolean.TRUE == used
        this.bankAccount = bankAccount
    }

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
    ) {
        this.id = id
        this.type = type
        this.created = created
        card = null
        bankAccount = null
        this.used = java.lang.Boolean.TRUE == used
        this.livemode = livemode
    }

    override fun hashCode(): Int {
        return Objects.hash(id, type, created, livemode, used, bankAccount, card)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is Token -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(token: Token): Boolean {
        return (id == token.id && type == token.type && created == token.created &&
            livemode == token.livemode && used == token.used &&
            bankAccount == token.bankAccount && card == token.card)
    }

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
            val liveModeOpt = StripeJsonUtils.optBoolean(jsonObject, FIELD_LIVEMODE)
            @TokenType val tokenType =
                asTokenType(StripeJsonUtils.optString(jsonObject, FIELD_TYPE))
            val usedOpt = StripeJsonUtils.optBoolean(jsonObject, FIELD_USED)
            if (tokenId == null || createdTimeStamp == null || liveModeOpt == null) {
                return null
            }

            val used = java.lang.Boolean.TRUE == usedOpt
            val liveMode = java.lang.Boolean.TRUE == liveModeOpt
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
            if (possibleTokenType == null || StripeTextUtils.isEmpty(possibleTokenType.trim { it <= ' ' })) {
                return null
            }

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

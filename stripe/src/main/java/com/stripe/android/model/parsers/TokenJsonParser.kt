package com.stripe.android.model.parsers

import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.model.Token
import com.stripe.android.model.Token.TokenType
import java.util.Date
import org.json.JSONObject

internal class TokenJsonParser : ModelJsonParser<Token> {
    override fun parse(json: JSONObject): Token? {
        val tokenId = StripeJsonUtils.optString(json, FIELD_ID)
        val createdTimeStamp = StripeJsonUtils.optLong(json, FIELD_CREATED)
        @Token.TokenType val tokenType =
            asTokenType(StripeJsonUtils.optString(json, FIELD_TYPE))
        if (tokenId == null || createdTimeStamp == null) {
            return null
        }

        val used = StripeJsonUtils.optBoolean(json, FIELD_USED)
        val liveMode = StripeJsonUtils.optBoolean(json, FIELD_LIVEMODE)
        val date = Date(createdTimeStamp * 1000)

        return if (TokenType.BANK_ACCOUNT == tokenType) {
            json.optJSONObject(FIELD_BANK_ACCOUNT)?.let {
                Token(
                    id = tokenId,
                    livemode = liveMode,
                    created = date,
                    used = used,
                    type = TokenType.BANK_ACCOUNT,
                    bankAccount = BankAccountJsonParser().parse(it)
                )
            }
        } else if (TokenType.CARD == tokenType) {
            json.optJSONObject(FIELD_CARD)?.let {
                Token(
                    id = tokenId,
                    livemode = liveMode,
                    created = date,
                    used = used,
                    type = TokenType.CARD,
                    card = CardJsonParser().parse(it)
                )
            }
        } else if (
            TokenType.PII == tokenType || TokenType.ACCOUNT == tokenType ||
            TokenType.CVC_UPDATE == tokenType || TokenType.PERSON == tokenType
        ) {
            Token(
                id = tokenId,
                type = tokenType,
                livemode = liveMode,
                created = date,
                used = used
            )
        } else {
            null
        }
    }

    private companion object {
        // The key for these object fields is identical to their retrieved values
        // from the Type field.
        private const val FIELD_BANK_ACCOUNT = TokenType.BANK_ACCOUNT
        private const val FIELD_CARD = TokenType.CARD
        private const val FIELD_CREATED = "created"
        private const val FIELD_ID = "id"
        private const val FIELD_LIVEMODE = "livemode"

        private const val FIELD_TYPE = "type"
        private const val FIELD_USED = "used"

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
                TokenType.PERSON -> TokenType.PERSON
                else -> null
            }
        }
    }
}

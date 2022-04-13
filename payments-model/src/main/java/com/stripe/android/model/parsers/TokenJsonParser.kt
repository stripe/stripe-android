package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.Token
import com.stripe.android.model.Token.Type
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TokenJsonParser : ModelJsonParser<Token> {
    override fun parse(json: JSONObject): Token? {
        val tokenId = StripeJsonUtils.optString(json, FIELD_ID)
        val createdTimeStamp = StripeJsonUtils.optLong(json, FIELD_CREATED)
        val tokenType = Type.fromCode(StripeJsonUtils.optString(json, FIELD_TYPE))
        if (tokenType == null || tokenId == null || createdTimeStamp == null) {
            return null
        }

        val used = StripeJsonUtils.optBoolean(json, FIELD_USED)
        val liveMode = StripeJsonUtils.optBoolean(json, FIELD_LIVEMODE)
        val date = Date(TimeUnit.SECONDS.toMillis(createdTimeStamp))

        return when (tokenType) {
            Type.Card -> {
                json.optJSONObject(Type.Card.code)?.let {
                    Token(
                        id = tokenId,
                        livemode = liveMode,
                        created = date,
                        used = used,
                        type = Type.Card,
                        card = CardJsonParser().parse(it)
                    )
                }
            }
            Type.BankAccount -> {
                json.optJSONObject(Type.BankAccount.code)?.let {
                    Token(
                        id = tokenId,
                        livemode = liveMode,
                        created = date,
                        used = used,
                        type = Type.BankAccount,
                        bankAccount = BankAccountJsonParser().parse(it)
                    )
                }
            }
            else -> {
                Token(
                    id = tokenId,
                    type = tokenType,
                    livemode = liveMode,
                    created = date,
                    used = used
                )
            }
        }
    }

    private companion object {
        private const val FIELD_CREATED = "created"
        private const val FIELD_ID = "id"
        private const val FIELD_LIVEMODE = "livemode"

        private const val FIELD_TYPE = "type"
        private const val FIELD_USED = "used"
    }
}

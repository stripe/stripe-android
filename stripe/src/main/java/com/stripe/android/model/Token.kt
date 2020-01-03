package com.stripe.android.model

import androidx.annotation.StringDef
import com.stripe.android.model.Token.TokenType
import com.stripe.android.model.parsers.TokenJsonParser
import java.util.Date
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Tokenization is the process Stripe uses to collect sensitive card, bank account details, Stripe
 * account details or personally identifiable information (PII), directly from your customers in a
 * secure manner. A Token representing this information is returned to you to use.
 */
@Parcelize
data class Token internal constructor(

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
) : StripeModel, StripePaymentSource {
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

    companion object {
        @JvmStatic
        fun fromString(jsonString: String?): Token? {
            if (jsonString == null) {
                return null
            }
            return try {
                fromJson(JSONObject(jsonString))
            } catch (exception: JSONException) {
                null
            }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Token? {
            return jsonObject?.let {
                TokenJsonParser().parse(it)
            }
        }
    }
}

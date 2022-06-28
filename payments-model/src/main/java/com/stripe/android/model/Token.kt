package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.Token.Type
import com.stripe.android.model.parsers.TokenJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Date

/**
 * Tokenization is the process Stripe uses to collect sensitive card, bank account details, Stripe
 * account details or personally identifiable information (PII), directly from your customers in a
 * secure manner. A Token representing this information is returned to you to use.
 */
@Parcelize
data class Token
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(

    /**
     * The Token id
     */
    override val id: String,

    /**
     * The [Type] of this token.
     */
    val type: Type,

    /**
     * The [Date] this token was created
     */
    val created: Date,

    /**
     * `true` if this token is valid for a real payment, `false` if it is only usable for testing
     */
    val livemode: Boolean,

    /**
     * `true` if this token has been used, `false` otherwise
     */
    val used: Boolean,

    /**
     * If applicable, the [BankAccount] for this token
     */
    val bankAccount: BankAccount? = null,

    /**
     * If applicable, the [Card] for this token
     */
    val card: Card? = null
) : StripeModel, StripePaymentSource {
    enum class Type(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val code: String
    ) {
        Card("card"),
        BankAccount("bank_account"),
        Pii("pii"),
        Account("account"),
        CvcUpdate("cvc_update"),
        Person("person");

        internal companion object {
            fun fromCode(code: String?) = values().firstOrNull { it.code == code }
        }
    }

    companion object {
        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Token? {
            return jsonObject?.let {
                TokenJsonParser().parse(it)
            }
        }
    }
}

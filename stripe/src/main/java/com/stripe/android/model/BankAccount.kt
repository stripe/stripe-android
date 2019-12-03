package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.Size
import androidx.annotation.StringDef
import com.stripe.android.model.parsers.BankAccountJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model class representing a bank account that can be used to create a token
 * via the protocol outlined in
 * [the Stripe
 * documentation.](https://stripe.com/docs/api/java#create_bank_account_token)
 */
@Parcelize
data class BankAccount internal constructor(
    val accountNumber: String?,
    val accountHolderName: String?,
    @param:BankAccountType @field:BankAccountType @get:BankAccountType
    val accountHolderType: String?,
    val bankName: String?,
    @param:Size(2) @field:Size(2) @get:Size(2)
    val countryCode: String?,
    @param:Size(3) @field:Size(3) @get:Size(3)
    val currency: String?,
    val fingerprint: String?,
    val last4: String?,
    val routingNumber: String?
) : StripeModel(), StripeParamsModel, Parcelable {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(BankAccountType.COMPANY, BankAccountType.INDIVIDUAL)
    annotation class BankAccountType {
        companion object {
            const val COMPANY: String = "company"
            const val INDIVIDUAL: String = "individual"
        }
    }

    /**
     * Constructor used to create a BankAccount object with the required parameters
     * to send to Stripe's server.
     *
     * @param accountNumber the account number for this BankAccount
     * @param countryCode the two-letter country code that this account was created in
     * @param currency the currency of this account
     * @param routingNumber the routing number of this account. Can be null for non US bank
     * accounts.
     */
    constructor(
        accountNumber: String,
        @Size(2) countryCode: String,
        @Size(3) currency: String,
        routingNumber: String?
    ) : this(
        accountNumber, null, null, null, countryCode,
        currency, null, null, routingNumber
    )

    /**
     * Constructor with no account number used internally to initialize an object
     * from JSON returned from the server.
     *
     * @param accountHolderName the account holder's name
     * @param accountHolderType the [BankAccountType]
     * @param bankName the name of the bank
     * @param countryCode the two-letter country code of the country in which the account was opened
     * @param currency the three-letter currency code
     * @param fingerprint the account fingerprint
     * @param last4 the last four digits of the account number
     * @param routingNumber the routing number of the bank
     */
    constructor(
        accountHolderName: String?,
        @BankAccountType accountHolderType: String?,
        bankName: String?,
        @Size(2) countryCode: String?,
        @Size(3) currency: String?,
        fingerprint: String?,
        last4: String?,
        routingNumber: String?
    ) : this(null, accountHolderName, accountHolderType, bankName, countryCode,
        currency, fingerprint, last4, routingNumber)

    override fun toParamMap(): Map<String, Any> {
        val accountParams = mapOf(
            "country" to countryCode,
            "currency" to currency,
            "account_number" to accountNumber,
            "routing_number" to routingNumber.takeUnless { it.isNullOrBlank() },
            "account_holder_name" to accountHolderName.takeUnless { it.isNullOrBlank() },
            "account_holder_type" to accountHolderType.takeUnless { it.isNullOrBlank() }
        )

        return mapOf(Token.TokenType.BANK_ACCOUNT to accountParams)
    }

    companion object {
        /**
         * Converts a String value into the appropriate [BankAccountType].
         *
         * @param possibleAccountType a String that might match a [BankAccountType] or be empty.
         * @return `null` if the input is blank or of unknown type, else the appropriate
         * [BankAccountType].
         */
        @BankAccountType
        fun asBankAccountType(possibleAccountType: String?): String? {
            return when (possibleAccountType) {
                BankAccountType.COMPANY -> BankAccountType.COMPANY
                BankAccountType.INDIVIDUAL -> BankAccountType.INDIVIDUAL
                else -> null
            }
        }

        fun fromJson(jsonObject: JSONObject): BankAccount {
            return BankAccountJsonParser().parse(jsonObject)
        }
    }
}

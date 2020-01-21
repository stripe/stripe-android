package com.stripe.android.model

import androidx.annotation.Size
import androidx.annotation.StringDef
import kotlinx.android.parcel.Parcelize

/**
 * [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account)
 */
@Parcelize
data class BankAccount internal constructor(
    val accountNumber: String? = null,
    val accountHolderName: String? = null,
    @param:BankAccountType @field:BankAccountType @get:BankAccountType
    val accountHolderType: String? = null,
    val bankName: String? = null,
    @param:Size(2) @field:Size(2) @get:Size(2)
    val countryCode: String? = null,
    @param:Size(3) @field:Size(3) @get:Size(3)
    val currency: String? = null,
    val fingerprint: String? = null,
    val last4: String? = null,
    val routingNumber: String? = null
) : StripeModel, StripeParamsModel {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(BankAccountType.COMPANY, BankAccountType.INDIVIDUAL)
    annotation class BankAccountType {
        companion object {
            const val COMPANY: String = "company"
            const val INDIVIDUAL: String = "individual"
        }
    }

    /**
     * [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account)
     *
     * @param accountNumber The account number for the bank account, in string form.
     * Must be a checking account.
     * @param countryCode The country in which the bank account is located.
     * @param currency The currency the bank account is in. This must be a country/currency pairing
     * that Stripe supports.
     * @param routingNumber Optional. The routing number, sort code, or other country-appropriate
     * institution number for the bank account. For US bank accounts, this is required and should
     * be the ACH routing number, not the wire routing number. If you are providing an IBAN for
     * `account_number`, this field is not required.
     * @param accountHolderName Optional. The name of the person or business that owns the bank
     * account. This field is required when attaching the bank account to a `Customer` object.
     * @param accountHolderType Optional. The type of entity that holds the account. This can be
     * either `individual` or `company`. This field is required when attaching the bank account to
     * a `Customer` object.
     */
    @JvmOverloads
    constructor(
        accountNumber: String,
        @Size(2) countryCode: String,
        @Size(3) currency: String,
        routingNumber: String? = null,
        accountHolderName: String? = null,
        @BankAccountType accountHolderType: String? = null
    ) : this(
        accountNumber = accountNumber,
        accountHolderName = accountHolderName,
        accountHolderType = accountHolderType,
        countryCode = countryCode,
        currency = currency,
        routingNumber = routingNumber,
        fingerprint = null
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
    @Deprecated("For internal use only")
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
        val bankAccountParams: Map<String, String> = listOf(
            "country" to countryCode,
            "currency" to currency,
            "account_number" to accountNumber,
            "routing_number" to routingNumber,
            "account_holder_name" to accountHolderName,
            "account_holder_type" to accountHolderType
        ).fold(emptyMap()) { acc, (key, value) ->
            acc.plus(
                value?.let { mapOf(key to it) }.orEmpty()
            )
        }

        return mapOf(Token.TokenType.BANK_ACCOUNT to bankAccountParams)
    }
}

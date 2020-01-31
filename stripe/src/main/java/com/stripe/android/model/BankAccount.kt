package com.stripe.android.model

import androidx.annotation.Size
import androidx.annotation.StringDef
import kotlinx.android.parcel.Parcelize

/**
 * [The bank account object](https://stripe.com/docs/api/customer_bank_accounts/object)
 */
@Parcelize
data class BankAccount internal constructor(
    /**
     * Unique identifier for the object.
     *
     * [id](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-id)
     */
    val id: String? = null,

    @Deprecated("Use BankAccountTokenParams")
    val accountNumber: String? = null,

    /**
     * The name of the person or business that owns the bank account.
     *
     * [account_holder_name](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-account_holder_name)
     */
    val accountHolderName: String? = null,

    /**
     * The type of entity that holds the account. This can be either individual or company.
     *
     * [account_holder_type](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-account_holder_type)
     */
    @param:BankAccountType @field:BankAccountType @get:BankAccountType
    val accountHolderType: String? = null,

    /**
     * Name of the bank associated with the routing number (e.g., WELLS FARGO).
     *
     * [bank_name](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-bank_name)
     */
    val bankName: String? = null,

    /**
     * Two-letter ISO code representing the country the bank account is located in.
     *
     * [country](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-country)
     */
    @param:Size(2) @field:Size(2) @get:Size(2)
    val countryCode: String? = null,

    /**
     * Three-letter ISO code for the currency paid out to the bank account.
     *
     * [currency](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-currency)
     */
    @param:Size(3) @field:Size(3) @get:Size(3)
    val currency: String? = null,

    /**
     * Uniquely identifies this particular bank account. You can use this attribute to check
     * whether two bank accounts are the same.
     *
     * [fingerprint](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-fingerprint)
     */
    val fingerprint: String? = null,

    /**
     * [last4](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-last4)
     */
    val last4: String? = null,

    /**
     * The routing transit number for the bank account.
     *
     * [routing_number](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-routing_number)
     */
    val routingNumber: String? = null,

    /**
     * For bank accounts, possible values are `new`, `validated`, `verified`, `verification_failed`,
     * or `errored`. A bank account that hasn’t had any activity or validation performed is new.
     * If Stripe can determine that the bank account exists, its status will be `validated`. Note
     * that there often isn’t enough information to know (e.g., for smaller credit unions), and
     * the validation is not always run. If customer bank account verification has succeeded,
     * the bank account status will be `verified`. If the verification failed for any reason,
     * such as microdeposit failure, the status will be `verification_failed`. If a transfer sent
     * to this bank account fails, we’ll set the status to `errored` and will not continue to send
     * transfers until the bank details are updated.
     *
     * For external accounts, possible values are `new` and `errored`. Validations aren’t run
     * against external accounts because they’re only used for payouts. This means the other
     * statuses don’t apply. If a transfer fails, the status is set to `errored` and transfers
     * are stopped until account details are updated.
     *
     * [status](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-status)
     */
    val status: Status? = null
) : StripeModel, TokenParams(Token.TokenType.BANK_ACCOUNT) {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(BankAccountType.COMPANY, BankAccountType.INDIVIDUAL)
    annotation class BankAccountType {
        companion object {
            const val COMPANY: String = "company"
            const val INDIVIDUAL: String = "individual"
        }
    }

    enum class Status(internal val code: String) {
        New("new"),
        Validated("validated"),
        Verified("verified"),
        VerificationFailed("verification_failed"),
        Errored("errored");

        internal companion object {
            internal fun fromCode(code: String?): Status? {
                return values().firstOrNull { it.code == code }
            }
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
    @Deprecated("Use BankAccountTokenParams")
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
    ) : this(
        id = null,
        accountHolderName = accountHolderName,
        accountHolderType = accountHolderType,
        bankName = bankName,
        countryCode = countryCode,
        currency = currency,
        fingerprint = fingerprint,
        last4 = last4,
        routingNumber = routingNumber
    )

    override fun toParamMap(): Map<String, Any> {
        return BankAccountTokenParams(
            country = countryCode.orEmpty(),
            currency = currency.orEmpty(),
            accountNumber = accountNumber.orEmpty(),
            routingNumber = routingNumber,
            accountHolderName = accountHolderName,
            accountHolderType = BankAccountTokenParams.Type.fromCode(accountHolderType)
        ).toParamMap()
    }
}

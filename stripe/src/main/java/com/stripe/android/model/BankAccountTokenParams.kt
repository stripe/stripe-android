package com.stripe.android.model

import kotlinx.parcelize.Parcelize

/**
 * [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account)
 */
@Parcelize
data class BankAccountTokenParams @JvmOverloads constructor(
    /**
     * The country in which the bank account is located.
     *
     * [bank_account.country](https://stripe.com/docs/api/tokens/create_bank_account#create_bank_account_token-bank_account-country)
     */
    private val country: String,

    /**
     * The currency the bank account is in. This must be a country/currency pairing that Stripe supports.
     *
     * [bank_account.currency](https://stripe.com/docs/api/tokens/create_bank_account#create_bank_account_token-bank_account-currency)
     */
    private val currency: String,

    /**
     * The account number for the bank account, in string form. Must be a checking account.
     *
     * [bank_account.account_number](https://stripe.com/docs/api/tokens/create_bank_account#create_bank_account_token-bank_account-account_number)
     */
    private val accountNumber: String,

    /**
     * The type of entity that holds the account. This can be either `individual` or `company`.
     * This field is required when attaching the bank account to a `Customer` object.
     *
     * [bank_account.account_holder_type](https://stripe.com/docs/api/tokens/create_bank_account#create_bank_account_token-bank_account-account_holder_type)
     */
    private val accountHolderType: Type? = null,

    /**
     * The name of the person or business that owns the bank account. This field is required when
     * attaching the bank account to a `Customer` object.
     *
     * [bank_account.account_holder_name](https://stripe.com/docs/api/tokens/create_bank_account#create_bank_account_token-bank_account-account_holder_name)
     */
    private val accountHolderName: String? = null,

    /**
     * The routing number, sort code, or other country-appropriate institution number for the
     * bank account. For US bank accounts, this is required and should be the ACH routing number,
     * not the wire routing number. If you are providing an IBAN for `account_number`,
     * this field is not required.
     *
     * [bank_account.routing_number](https://stripe.com/docs/api/tokens/create_bank_account#create_bank_account_token-bank_account-routing_number)
     */
    private val routingNumber: String? = null
) : TokenParams(Token.Type.BankAccount) {
    enum class Type(internal val code: String) {
        Individual("individual"),
        Company("company");

        internal companion object {
            @JvmSynthetic
            internal fun fromCode(code: String?): Type? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    override val typeDataParams: Map<String, Any>
        get() = listOf(
            PARAM_COUNTRY to country,
            PARAM_CURRENCY to currency,
            PARAM_ACCOUNT_HOLDER_NAME to accountHolderName,
            PARAM_ACCOUNT_HOLDER_TYPE to accountHolderType?.code,
            PARAM_ROUTING_NUMBER to routingNumber,
            PARAM_ACCOUNT_NUMBER to accountNumber
        ).fold(emptyMap()) { acc, (key, value) ->
            acc.plus(
                value?.let { mapOf(key to it) }.orEmpty()
            )
        }

    private companion object {
        private const val PARAM_COUNTRY = "country"
        private const val PARAM_CURRENCY = "currency"
        private const val PARAM_ACCOUNT_HOLDER_NAME = "account_holder_name"
        private const val PARAM_ACCOUNT_HOLDER_TYPE = "account_holder_type"
        private const val PARAM_ROUTING_NUMBER = "routing_number"
        private const val PARAM_ACCOUNT_NUMBER = "account_number"
    }
}

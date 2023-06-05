package com.stripe.android.model

import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.annotation.Size
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * [The bank account object](https://stripe.com/docs/api/customer_bank_accounts/object)
 */
@Parcelize
data class BankAccount
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * Unique identifier for the object.
     *
     * [id](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-id)
     */
    override val id: String? = null,

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
    val accountHolderType: Type? = null,

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
    @param:Size(2)
    @field:Size(2)
    @get:Size(2)
    val countryCode: String? = null,

    /**
     * Three-letter ISO code for the currency paid out to the bank account.
     *
     * [currency](https://stripe.com/docs/api/customer_bank_accounts/object#customer_bank_account_object-currency)
     */
    @param:Size(3)
    @field:Size(3)
    @get:Size(3)
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
) : StripeModel, StripePaymentSource {

    enum class Type(internal val code: String) {
        Company("company"),
        Individual("individual");

        @Keep
        override fun toString(): String = code

        internal companion object {
            internal fun fromCode(code: String?) = values().firstOrNull { it.code == code }
        }
    }

    enum class Status(internal val code: String) {
        New("new"),
        Validated("validated"),
        Verified("verified"),
        VerificationFailed("verification_failed"),
        Errored("errored");

        @Keep
        override fun toString(): String = code

        internal companion object {
            internal fun fromCode(code: String?): Status? {
                return values().firstOrNull { it.code == code }
            }
        }
    }
}

package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A FinancialConnectionsAccount represents an account that exists outside of Stripe,
 * to which you have been granted some degree of access.
 *
 * @param category
 * @param created Time at which the object was created. Measured in seconds since the Unix epoch.
 * @param id Unique identifier for the object.
 * @param institutionName The name of the institution that holds this account.
 * @param livemode Has the value `true` if the object exists in live mode or the value `false` if
 * the object exists in test mode.
 * @param status The status of the link to the account.
 * @param subcategory If `category` is `cash`, one of:   - `checking`  - `savings`  - `other`
 * If `category` is `credit`, one of:   - `mortgage`  - `line_of_credit`  - `credit_card`  - `other`
 * If `category` is `investment` or `other`, this will be `other`.
 * @param supportedPaymentMethodTypes
 * The [PaymentMethod type](https://stripe.com/docs/api/payment_methods/object#payment_method_object-type)(s)
 * that can be created from this FinancialConnectionsAccount.
 * @param balance The most recent information about the account's balance.
 * @param balanceRefresh The state of the most recent attempt to refresh the account balance.
 * @param displayName A human-readable name that has been assigned to this account,
 * either by the account holder or by the institution.
 * @param last4 The last 4 digits of the account number. If present, this will be 4 numeric characters.
 * @param ownership The most recent information about the account's owners.
 * @param ownershipRefresh The state of the most recent attempt to refresh the account owners.
 * @param permissions The list of permissions granted by this account.
 */
@Serializable
@Parcelize
@Suppress("unused")
data class FinancialConnectionsAccount(

    @SerialName("category")
    val category: Category = Category.UNKNOWN,

    /* Time at which the object was created. Measured in seconds since the Unix epoch. */
    @SerialName("created")
    val created: Int,

    /* Unique identifier for the object. */
    @SerialName("id")
    val id: String,

    /* The name of the institution that holds this account. */
    @SerialName("institution_name")
    val institutionName: String,

    /* Has the value `true` if the object exists in live mode or the value `false` if the object exists in test mode. */
    @SerialName("livemode")
    val livemode: Boolean,

    /* The status of the link to the account. */
    @SerialName("status")
    val status: Status = Status.UNKNOWN,

    /* If `category` is `cash`, one of:   - `checking`  - `savings`  - `other`
       If `category` is `credit`, one of:   - `mortgage`  - `line_of_credit`  - `credit_card`  - `other`
       If `category` is `investment` or `other`, this will be `other`.
    */
    @SerialName("subcategory")
    val subcategory: Subcategory = Subcategory.UNKNOWN,

    /* The [PaymentMethod type](https://stripe.com/docs/api/payment_methods/object#payment_method_object-type)(s)
     that can be created from this FinancialConnectionsAccount. */
    @SerialName("supported_payment_method_types")
    val supportedPaymentMethodTypes: List<SupportedPaymentMethodTypes>,

    /* The most recent information about the account's balance. */
    @SerialName("balance")
    val balance: Balance? = null,

    /* The state of the most recent attempt to refresh the account balance. */
    @SerialName("balance_refresh")
    val balanceRefresh: BalanceRefresh? = null,

    /* A human-readable name that has been assigned to this account,
    either by the account holder or by the institution. */
    @SerialName("display_name")
    val displayName: String? = null,

    /* The last 4 digits of the account number. If present, this will be 4 numeric characters. */
    @SerialName("last4")
    val last4: String? = null,

    /* The most recent information about the account's owners. */
    @SerialName("ownership")
    val ownership: String? = null,

    /* The state of the most recent attempt to refresh the account owners. */
    @SerialName("ownership_refresh")
    val ownershipRefresh: OwnershipRefresh? = null,

    /* The list of permissions granted by this account. */
    @SerialName("permissions")
    val permissions: List<Permissions>? = null

) : StripeModel, Parcelable, PaymentAccount() {

    /**
     *
     *
     * Values: cash,credit,investment,other
     */
    @Serializable(with = Category.Serializer::class)
    enum class Category(val value: String) {
        @SerialName("cash")
        CASH("cash"),

        @SerialName("credit")
        CREDIT("credit"),

        @SerialName("investment")
        INVESTMENT("investment"),

        @SerialName("other")
        OTHER("other"),

        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<Category>(Category.values(), UNKNOWN)
    }

    /**
     * The status of the link to the account.
     *
     * Values: active,disconnected,inactive
     */
    @Serializable(with = Status.Serializer::class)
    enum class Status(val value: String) {
        @SerialName("active")
        ACTIVE("active"),

        @SerialName("disconnected")
        DISCONNECTED("disconnected"),

        @SerialName("inactive")
        INACTIVE("inactive"),

        UNKNOWN("unknown");

        internal object Serializer : EnumIgnoreUnknownSerializer<Status>(Status.values(), UNKNOWN)
    }

    /**
     * If `category` is `cash`, one of:   - `checking`  - `savings`  - `other`
     * If `category` is `credit`, one of:   - `mortgage`  - `line_of_credit`  - `credit_card`  - `other`
     * If `category` is `investment` or `other`, this will be `other`.
     *
     * Values: checking,creditCard,lineOfCredit,mortgage,other,savings
     */
    @Serializable(with = Subcategory.Serializer::class)
    enum class Subcategory(val value: String) {
        @SerialName("checking")
        CHECKING("checking"),

        @SerialName("credit_card")
        CREDIT_CARD("credit_card"),

        @SerialName("line_of_credit")
        LINE_OF_CREDIT("line_of_credit"),

        @SerialName("mortgage")
        MORTGAGE("mortgage"),

        @SerialName("other")
        OTHER("other"),

        @SerialName("savings")
        SAVINGS("savings"),

        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<Subcategory>(Subcategory.values(), UNKNOWN)
    }

    /**
     * The [PaymentMethod type](https://stripe.com/docs/api/payment_methods/object#payment_method_object-type)(s)
     * that can be created from this FinancialConnectionsAccount.
     *
     * Values: link,usBankAccount
     */
    @Serializable(with = SupportedPaymentMethodTypes.Serializer::class)
    enum class SupportedPaymentMethodTypes(val value: String) {
        @SerialName("link")
        LINK("link"),

        @SerialName("us_bank_account")
        US_BANK_ACCOUNT("us_bank_account"),

        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<SupportedPaymentMethodTypes>(values(), UNKNOWN)
    }

    /**
     * The list of permissions granted by this account.
     *
     * Values: balances,identity,ownership,paymentMethod,transactions
     */
    @Serializable(with = Permissions.Serializer::class)
    enum class Permissions(val value: String) {
        @SerialName("balances")
        BALANCES("balances"),

        @SerialName("ownership")
        OWNERSHIP("ownership"),

        @SerialName("payment_method")
        PAYMENT_METHOD("payment_method"),

        @SerialName("transactions")
        TRANSACTIONS("transactions"),

        @SerialName("account_numbers")
        ACCOUNT_NUMBERS("account_numbers"),

        UNKNOWN("unknown");

        internal object Serializer : EnumIgnoreUnknownSerializer<Permissions>(values(), UNKNOWN)
    }

    companion object {
        internal const val OBJECT_OLD = "linked_account"
        internal const val OBJECT_NEW = "financial_connections.account"
    }
}

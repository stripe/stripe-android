package com.stripe.android.paymentsheet.stripebalance

import android.os.Parcelable
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A buyer's financial account that a Stripe balance payment can be made from.
 */
@Parcelize
internal data class StripeBalanceFinancialAccount(
    val id: String,
    val displayName: String?,
    /** Available balances in the currency's minor unit, keyed by lowercased currency code. */
    val availableBalances: Map<String, Long>,
    /** Whether the account can cover the amount, or `null` without an amount, e.g. when setting up. */
    val eligible: Boolean?,
    /** Why the account can't cover the amount, when [eligible] is `false`. */
    val ineligibleReason: IneligibleReason?,
) : StripeModel {

    /** Accounts are selectable unless the server says they can't cover the amount. */
    val isSelectable: Boolean
        get() = eligible != false

    /** The available balance in [currency], if the account holds any. */
    fun availableBalance(currency: String): Long? {
        return availableBalances[currency.lowercase()]
    }

    sealed interface IneligibleReason : Parcelable {
        /** The account holds the currency, but less than the amount. */
        @Parcelize
        data object InsufficientBalance : IneligibleReason

        /** The account doesn't hold the currency. */
        @Parcelize
        data object CurrencyMismatch : IneligibleReason

        /** A reason this version of the SDK doesn't know about. */
        @Parcelize
        data class Other(val value: String) : IneligibleReason

        companion object {
            fun fromValue(value: String): IneligibleReason {
                return when (value) {
                    "insufficient_balance" -> InsufficientBalance
                    "currency_mismatch" -> CurrencyMismatch
                    else -> Other(value)
                }
            }
        }
    }
}

/**
 * The buyer's financial accounts, loaded with the Payment Element.
 */
@Parcelize
internal data class StripeBalanceFinancialAccounts(
    val accounts: List<StripeBalanceFinancialAccount>,
    /** The currency the buyer pays in, used to show each account's balance, or `null` when setting up. */
    val currency: String?,
) : Parcelable

@Parcelize
internal data class StripeBalanceFinancialAccountList(
    val data: List<StripeBalanceFinancialAccount>,
) : StripeModel

internal object StripeBalanceFinancialAccountListJsonParser : ModelJsonParser<StripeBalanceFinancialAccountList> {
    override fun parse(json: JSONObject): StripeBalanceFinancialAccountList? {
        val data = json.optJSONArray("data") ?: return null
        // Like web and iOS, a malformed account fails the whole response, so Stripe balance isn't offered
        val accounts = (0 until data.length()).map { index ->
            data.optJSONObject(index)?.let(::parseFinancialAccount) ?: return null
        }
        return StripeBalanceFinancialAccountList(accounts)
    }

    private fun parseFinancialAccount(json: JSONObject): StripeBalanceFinancialAccount? {
        val id = StripeJsonUtils.optString(json, "id") ?: return null
        val available = json.optJSONObject("balance")?.optJSONObject("available") ?: return null
        val availableBalances = available.keys().asSequence().associate { currency ->
            val value = available.optJSONObject(currency)?.opt("value") as? Number ?: return null
            currency.lowercase() to value.toLong()
        }
        return StripeBalanceFinancialAccount(
            id = id,
            displayName = StripeJsonUtils.optString(json, "display_name"),
            availableBalances = availableBalances,
            eligible = if (json.isNull("eligible")) null else json.getBoolean("eligible"),
            ineligibleReason = StripeJsonUtils.optString(json, "ineligible_reason")
                ?.let(StripeBalanceFinancialAccount.IneligibleReason::fromValue),
        )
    }
}

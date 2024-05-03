package com.stripe.android.financialconnections.features.manualentry

import androidx.annotation.StringRes
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.manualentry.Validator.getAccountConfirmIdOrNull
import com.stripe.android.financialconnections.features.manualentry.Validator.getAccountErrorIdOrNull
import com.stripe.android.financialconnections.features.manualentry.Validator.getRoutingErrorIdOrNull

internal class ManualEntryFormState(
    private val routing: String? = null,
    private val account: String? = null,
    private val accountConfirm: String? = null,
) {
    val routingError: Int?
        get() = routing?.let { getRoutingErrorIdOrNull(routing) }

    val accountError: Int?
        get() = account?.let { getAccountErrorIdOrNull(account) }

    val accountConfirmError: Int?
        get() = if (account != null && accountConfirm != null) {
            getAccountConfirmIdOrNull(account, accountConfirm)
        } else {
            null
        }

    val isValid: Boolean
        get() = routing != null && account != null && accountConfirm != null &&
            routingError == null &&
            accountError == null &&
            accountConfirmError == null
}

private object Validator {

    @StringRes
    fun getRoutingErrorIdOrNull(input: String): Int? = when {
        input.isEmpty() -> R.string.stripe_validation_routing_required
        input.length != ROUTING_NUMBER_LENGTH -> R.string.stripe_validation_routing_too_short
        input.isUSRoutingNumber().not() -> R.string.stripe_validation_no_us_routing
        else -> null
    }

    @StringRes
    fun getAccountErrorIdOrNull(input: String): Int? = when {
        input.isEmpty() -> R.string.stripe_validation_account_required
        input.length > ACCOUNT_NUMBER_MAX_LENGTH -> R.string.stripe_validation_account_too_long
        else -> null
    }

    @StringRes
    fun getAccountConfirmIdOrNull(
        accountInput: String,
        accountConfirmInput: String
    ): Int? = when {
        getAccountErrorIdOrNull(accountInput) == null &&
            accountInput != accountConfirmInput -> R.string.stripe_validation_account_confirm_mismatch

        else -> null
    }

    @Suppress("MagicNumber")
    private fun String.isUSRoutingNumber(): Boolean {
        val usRoutingFactor: (Int) -> Int = {
            when (it % 3) {
                0 -> 3
                1 -> 7
                else -> 1
            }
        }
        return if (Regex("^\\d{9}\$").matches(this)) {
            val total = foldIndexed(0) { idx: Int, sum: Int, current: Char ->
                sum + current.digitToInt(10) * usRoutingFactor(idx)
            }
            total % 10 == 0
        } else {
            false
        }
    }

    private const val ROUTING_NUMBER_LENGTH = 9
    private const val ACCOUNT_NUMBER_MAX_LENGTH = 17
}

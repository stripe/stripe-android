package com.stripe.android.financialconnections.features.manualentry

import com.stripe.android.financialconnections.R

internal object ManualEntryInputValidator {

    fun getRoutingErrorIdOrNull(input: String): Int? = when {
        input.isEmpty() -> R.string.stripe_validation_routing_required
        input.length != ROUTING_NUMBER_LENGTH -> R.string.stripe_validation_routing_too_short
        input.isUSRoutingNumber().not() -> R.string.stripe_validation_no_us_routing
        else -> null
    }

    fun getAccountErrorIdOrNull(input: String): Int? = when {
        input.isEmpty() -> R.string.stripe_validation_account_required
        input.length > ACCOUNT_NUMBER_MAX_LENGTH -> R.string.stripe_validation_account_too_long
        else -> null
    }

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

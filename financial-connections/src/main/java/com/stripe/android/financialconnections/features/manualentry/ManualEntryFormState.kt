package com.stripe.android.financialconnections.features.manualentry

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.stripe.android.financialconnections.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class ManualEntryFormState(
    scope: CoroutineScope
) {

    private val started = SharingStarted.WhileSubscribed(5_000)

    var routing: String? by mutableStateOf(null)
    val routingError: StateFlow<Int?> = snapshotFlow { routing }
        .filterNotNull()
        .mapLatest { getRoutingErrorIdOrNull(it) }
        .stateIn(
            scope = scope,
            started = started,
            initialValue = null
        )

    var account: String? by mutableStateOf(null)
    val accountError: StateFlow<Int?> = snapshotFlow { account }
        .filterNotNull()
        .mapLatest { getAccountErrorIdOrNull(it) }
        .stateIn(
            scope = scope,
            started = started,
            initialValue = null
        )

    var accountConfirm: String? by mutableStateOf(null)
    val accountConfirmError: StateFlow<Int?> = combine(
        snapshotFlow { accountConfirm }.filterNotNull(),
        snapshotFlow { account }.filterNotNull()
    ) { accountConfirm, account ->
        getAccountConfirmIdOrNull(account, accountConfirm)
    }.stateIn(
        scope = scope,
        started = started,
        initialValue = null
    )

    /**
     * The form is valid if all error states are null (no errors)
     * and all fields are non-null (user has entered a value at any point on all fields)
     */
    val isValid: StateFlow<Boolean> = combine(
        routingError,
        accountError,
        accountConfirmError,
    ) { routingErr, accountErr, accountConfirmErr ->
        // The form is valid if all error states are null (no errors)
        routing != null && account != null && accountConfirm != null &&
            routingErr == null && accountErr == null && accountConfirmErr == null
    }.stateIn(
        scope = scope,
        started = started,
        initialValue = false
    )

    @StringRes
    private fun getRoutingErrorIdOrNull(input: String): Int? = when {
        input.isEmpty() -> R.string.stripe_validation_routing_required
        input.length != ROUTING_NUMBER_LENGTH -> R.string.stripe_validation_routing_too_short
        input.isUSRoutingNumber().not() -> R.string.stripe_validation_no_us_routing
        else -> null
    }

    @StringRes
    private fun getAccountErrorIdOrNull(input: String): Int? = when {
        input.isEmpty() -> R.string.stripe_validation_account_required
        input.length > ACCOUNT_NUMBER_MAX_LENGTH -> R.string.stripe_validation_account_too_long
        else -> null
    }

    @StringRes
    private fun getAccountConfirmIdOrNull(
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

    companion object {
        private const val ROUTING_NUMBER_LENGTH = 9
        private const val ACCOUNT_NUMBER_MAX_LENGTH = 17
    }
}

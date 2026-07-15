package com.stripe.link.feature.paymentmethod

private const val SET_DEFAULT_ACTION_ID = "set_default"

/**
 * Maps domain state to the UI payload for the payment method screen.
 */
class PaymentMethodStatePayloadMapper {

    fun buildSetDefaultAction(
        paymentMethod: PaymentMethodItem,
        onToggleAsDefault: (Boolean) -> Unit,
    ): PaymentMethodAction {
        return activityAction(
            id = SET_DEFAULT_ACTION_ID,
            title = "Set as default",
            switchState = SwitchState(checked = paymentMethod.isDefault),
            // The row itself is non-interactive for toggle rows; the LinkSwitch
            // drives the toggle via onToggle.
            onClick = {},
            onToggle = onToggleAsDefault,
        )
    }

    fun buildActivityActionsGroup(
        accounts: List<ActivityAccount>,
        onToggle: (String, Boolean) -> Unit,
    ): List<PaymentMethodAction> {
        return accounts.map { account ->
            when (account.status) {
                AccountStatus.Active -> activityAction(
                    id = account.id,
                    title = account.name,
                    switchState = SwitchState(checked = account.subscribedToTransactions),
                    // The row itself is non-interactive for toggle rows; the LinkSwitch
                    // drives the toggle via onToggle.
                    onClick = {},
                    onToggle = { enabled -> onToggle(account.id, enabled) },
                )
                AccountStatus.Inactive -> activityAction(
                    id = account.id,
                    title = account.name,
                    switchState = null,
                    onClick = { /* no-op for inactive accounts */ },
                    onToggle = null,
                )
            }
        }
    }

    private fun activityAction(
        id: String,
        title: String,
        switchState: SwitchState?,
        onClick: () -> Unit,
        onToggle: ((Boolean) -> Unit)?,
    ) = PaymentMethodAction(
        id = id,
        title = title,
        switchState = switchState,
        onClick = onClick,
        onToggle = onToggle,
    )
}

// ---------------------------------------------------------------------------
// Domain models used by the mapper
// ---------------------------------------------------------------------------

data class PaymentMethodItem(
    val id: String,
    val isDefault: Boolean,
)

data class SwitchState(
    val checked: Boolean,
    val enabled: Boolean = true,
)

data class PaymentMethodAction(
    val id: String,
    val title: String,
    val switchState: SwitchState?,
    val onClick: () -> Unit,
    val onToggle: ((Boolean) -> Unit)?,
    val triggersAlertHapticStack: Boolean = false,
    val leadingIcon: Any? = null,
    val trailingIndicator: Any? = null,
)

data class ActivityAccount(
    val id: String,
    val name: String,
    val status: AccountStatus,
    val subscribedToTransactions: Boolean,
)

enum class AccountStatus { Active, Inactive }

package com.stripe.android.financialconnections.model

/**
 * Updates the [FinancialConnectionsSession] with any data from the [FinancialConnectionsSessionManifest]
 * that's not already present.
 */
internal fun FinancialConnectionsSession.update(
    manifest: FinancialConnectionsSessionManifest?,
): FinancialConnectionsSession {
    val manualEntryUsesMicrodeposits = manifest?.manualEntryUsesMicrodeposits ?: false
    return copy(
        paymentAccount = paymentAccount?.setUsesMicrodepositsIfNeeded(manualEntryUsesMicrodeposits),
    )
}

/**
 * Updates the [FinancialConnectionsSession] with the [usesMicrodeposits] value if the linked account is
 * a [BankAccount].
 */
internal fun FinancialConnectionsSession.update(
    usesMicrodeposits: Boolean,
): FinancialConnectionsSession {
    return copy(
        paymentAccount = paymentAccount?.setUsesMicrodepositsIfNeeded(usesMicrodeposits),
    )
}

private fun PaymentAccount.setUsesMicrodepositsIfNeeded(
    usesMicrodeposits: Boolean,
): PaymentAccount {
    return when (this) {
        is BankAccount -> copy(usesMicrodeposits = usesMicrodeposits)
        is FinancialConnectionsAccount -> this
    }
}

package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class PermissionsSetting(
    override val selectedOption: List<Permission> = listOf(Permission.Balances),
    override val key: String = "permissions",
) : Saveable<List<Permission>>, MultipleChoiceSetting<Permission>(
    displayName = "Permissions",
    options = Permission.values().map { Option(it.name, it) },
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        permissions = convertToString(selectedOption)
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        permissions = convertToString(selectedOption)
    )

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: List<Permission>): List<Setting<*>> =
        replace(currentSettings, this.copy(selectedOption = value))

    override fun convertToString(value: List<Permission>): String {
        return value.joinToString(",") { it.apiValue }
    }

    override fun convertToValue(value: String): List<Permission> = if (value.isEmpty()) {
        emptyList()
    } else {
        value.split(",").map { Permission.fromApiValue(it) }.toList()
    }
}

enum class Permission(val apiValue: String) {
    Balances("balances"),
    Transactions("transactions"),
    Ownership("ownership"),
    PaymentMethod("payment_method");

    companion object {
        fun fromApiValue(value: String): Permission = values().first { it.apiValue == value }
    }
}

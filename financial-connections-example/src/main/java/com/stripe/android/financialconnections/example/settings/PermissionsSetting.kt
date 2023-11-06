package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class PermissionsSetting(
    override val selectedOption: List<Permission> = listOf(Permission.Read),
    override val key: String = "permissions",
) : Saveable<List<Permission>>, MultipleChoiceSetting<Permission>(
    displayName = "Flow",
    options = Permission.values().map { Option(it.name, it) },
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: List<Permission>): List<Setting<*>> =
        replace(currentSettings, this.copy(selectedOption = value))

    override fun convertToString(value: List<Permission>): String {
        return value.joinToString(",")
    }

    override fun convertToValue(value: String): List<Permission> {
        return value.split(",").map { Permission.valueOf(it) }.toList()
    }

}

enum class Permission {
    Read,
    Write;
}
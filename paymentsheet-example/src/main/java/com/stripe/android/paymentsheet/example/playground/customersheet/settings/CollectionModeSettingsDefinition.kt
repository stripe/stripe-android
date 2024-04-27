package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

internal open class CollectionModeSettingsDefinition(
    override val key: String,
    override val displayName: String
) : CustomerSheetPlaygroundSettingDefinition.Saveable<CollectionMode>,
    CustomerSheetPlaygroundSettingDefinition.Displayable<CollectionMode> {

    override val defaultValue: CollectionMode = CollectionMode.Automatic
    override val options: List<CustomerSheetPlaygroundSettingDefinition.Displayable.Option<CollectionMode>> by lazy {
        listOf(
            option("Auto", CollectionMode.Automatic),
            option("Never", CollectionMode.Never),
            option("Always", CollectionMode.Always),
        )
    }

    override fun convertToString(value: CollectionMode): String = when (value) {
        CollectionMode.Automatic -> "auto"
        CollectionMode.Never -> "never"
        CollectionMode.Always -> "always"
    }

    override fun convertToValue(value: String): CollectionMode = when (value) {
        "auto" -> CollectionMode.Automatic
        "never" -> CollectionMode.Never
        "always" -> CollectionMode.Always
        else -> CollectionMode.Automatic
    }
}

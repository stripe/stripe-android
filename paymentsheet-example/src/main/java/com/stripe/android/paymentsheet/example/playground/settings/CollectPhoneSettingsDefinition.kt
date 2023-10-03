package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object CollectPhoneSettingsDefinition : PlaygroundSettingDefinition<CollectionMode>(
    key = "collectPhone",
    displayName = "Collect Phone",
) {
    override val defaultValue: CollectionMode = CollectionMode.Automatic
    override val options: List<Option<CollectionMode>> = listOf(
        Option("Auto", CollectionMode.Automatic),
        Option("Never", CollectionMode.Never),
        Option("Always", CollectionMode.Always),
    )

    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(phone = value) }
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

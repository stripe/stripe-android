package com.stripe.android.paymentsheet.example.playground.settings

internal object ElementsShippingSettingsDefinition :
    PlaygroundSettingDefinition<ElementsShipping>,
    PlaygroundSettingDefinition.Saveable<ElementsShipping> by EnumSaveable(
        key = "expressCheckoutShipping",
        values = ElementsShipping.entries.toTypedArray(),
        defaultValue = ElementsShipping.Off,
    ),
    PlaygroundSettingDefinition.Displayable<ElementsShipping> {
    override val displayName = "Elements: shipping"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean = configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.ElementsController

    override fun createOptions(configurationData: PlaygroundConfigurationData) = listOf(
        option("Off", ElementsShipping.Off),
        option("Accept", ElementsShipping.Accept),
        option("Reject", ElementsShipping.Reject),
    )
}

internal enum class ElementsShipping : ValueEnum {
    Off,
    Accept,
    Reject;

    override val value: String
        get() = name
}

internal object ElementsWalletOrderSettingsDefinition :
    PlaygroundSettingDefinition<ElementsWalletOrder>,
    PlaygroundSettingDefinition.Saveable<ElementsWalletOrder> by EnumSaveable(
        key = "expressCheckoutWalletOrder",
        values = ElementsWalletOrder.entries.toTypedArray(),
        defaultValue = ElementsWalletOrder.Session,
    ),
    PlaygroundSettingDefinition.Displayable<ElementsWalletOrder> {
    override val displayName = "Elements: Express Checkout wallet order"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean = configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.ElementsController

    override fun createOptions(configurationData: PlaygroundConfigurationData) = listOf(
        option("Session", ElementsWalletOrder.Session),
        option("Google Pay first", ElementsWalletOrder.GooglePayFirst),
        option("Link first", ElementsWalletOrder.LinkFirst),
    )
}

internal enum class ElementsWalletOrder : ValueEnum {
    Session,
    GooglePayFirst,
    LinkFirst;

    override val value: String
        get() = name
}

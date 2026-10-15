package com.stripe.android.paymentsheet.example.playground.settings

internal object ExpressCheckoutShippingSettingsDefinition :
    PlaygroundSettingDefinition<ExpressCheckoutShipping>,
    PlaygroundSettingDefinition.Saveable<ExpressCheckoutShipping> by EnumSaveable(
        key = "expressCheckoutShipping",
        values = ExpressCheckoutShipping.entries.toTypedArray(),
        defaultValue = ExpressCheckoutShipping.Off,
    ),
    PlaygroundSettingDefinition.Displayable<ExpressCheckoutShipping> {
    override val displayName = "Express Checkout shipping"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean = configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.ExpressCheckoutElement

    override fun createOptions(configurationData: PlaygroundConfigurationData) = listOf(
        option("Off", ExpressCheckoutShipping.Off),
        option("Accept", ExpressCheckoutShipping.Accept),
        option("Reject", ExpressCheckoutShipping.Reject),
    )
}

internal enum class ExpressCheckoutShipping : ValueEnum {
    Off,
    Accept,
    Reject;

    override val value: String
        get() = name
}

internal object ExpressCheckoutWalletOrderSettingsDefinition :
    PlaygroundSettingDefinition<ExpressCheckoutWalletOrder>,
    PlaygroundSettingDefinition.Saveable<ExpressCheckoutWalletOrder> by EnumSaveable(
        key = "expressCheckoutWalletOrder",
        values = ExpressCheckoutWalletOrder.entries.toTypedArray(),
        defaultValue = ExpressCheckoutWalletOrder.Session,
    ),
    PlaygroundSettingDefinition.Displayable<ExpressCheckoutWalletOrder> {
    override val displayName = "Express Checkout wallet order"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean = configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.ExpressCheckoutElement

    override fun createOptions(configurationData: PlaygroundConfigurationData) = listOf(
        option("Session", ExpressCheckoutWalletOrder.Session),
        option("Google Pay first", ExpressCheckoutWalletOrder.GooglePayFirst),
        option("Link first", ExpressCheckoutWalletOrder.LinkFirst),
    )
}

internal enum class ExpressCheckoutWalletOrder : ValueEnum {
    Session,
    GooglePayFirst,
    LinkFirst;

    override val value: String
        get() = name
}

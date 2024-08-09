package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.AllowRedisplayFilter
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomerSessionOverrideRedisplaySettingsDefinition :
    PlaygroundSettingDefinition<CustomerSessionOverrideRedisplaySettingsDefinition.OverrideAllowRedisplay>,
    PlaygroundSettingDefinition.Saveable<CustomerSessionOverrideRedisplaySettingsDefinition.OverrideAllowRedisplay>
    by EnumSaveable(
        key = "customer_session_payment_method_override_redisplay",
        values = CustomerSessionOverrideRedisplaySettingsDefinition.OverrideAllowRedisplay.entries.toTypedArray(),
        defaultValue = OverrideAllowRedisplay.NotSet,
    ),
    PlaygroundSettingDefinition.Displayable<CustomerSessionOverrideRedisplaySettingsDefinition.OverrideAllowRedisplay> {
    override val displayName: String = "Customer Session Override Allow Redisplay"

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        PlaygroundSettingDefinition.Displayable.Option("Not Set", OverrideAllowRedisplay.NotSet),
        PlaygroundSettingDefinition.Displayable.Option("Always", OverrideAllowRedisplay.Always),
        PlaygroundSettingDefinition.Displayable.Option("Limited", OverrideAllowRedisplay.Limited),
        PlaygroundSettingDefinition.Displayable.Option("Unspecified", OverrideAllowRedisplay.Unspecified),
    )

    override fun configure(value: OverrideAllowRedisplay, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.paymentMethodOverrideRedisplay(value.filter)
    }

    enum class OverrideAllowRedisplay(override val value: String, val filter: AllowRedisplayFilter?) : ValueEnum {
        Always("always", AllowRedisplayFilter.Always),
        Limited("limited", AllowRedisplayFilter.Limited),
        Unspecified("unspecified", AllowRedisplayFilter.Unspecified),
        NotSet("notSet", null)
    }
}

package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.AllowRedisplayFilter
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest

internal object CustomerSessionRedisplayFiltersSettingsDefinition :
    PlaygroundSettingDefinition<CustomerSessionRedisplayFiltersSettingsDefinition.RedisplayFilterType>,
    PlaygroundSettingDefinition.Saveable<CustomerSessionRedisplayFiltersSettingsDefinition.RedisplayFilterType>
    by EnumSaveable(
        key = "customer_session_payment_method_redisplay_filters",
        values = RedisplayFilterType.entries.toTypedArray(),
        defaultValue = RedisplayFilterType.Always,
    ),
    PlaygroundSettingDefinition.Displayable<CustomerSessionRedisplayFiltersSettingsDefinition.RedisplayFilterType> {
    override val displayName: String = "Customer Session Allow Redisplay Filters"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        PlaygroundSettingDefinition.Displayable.Option("All", RedisplayFilterType.All),
        PlaygroundSettingDefinition.Displayable.Option("Always", RedisplayFilterType.Always),
        PlaygroundSettingDefinition.Displayable.Option("Limited", RedisplayFilterType.Limited),
        PlaygroundSettingDefinition.Displayable.Option("Unspecified", RedisplayFilterType.Unspecified),
        PlaygroundSettingDefinition.Displayable.Option("None", RedisplayFilterType.None),
    )

    override fun configure(value: RedisplayFilterType, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.paymentMethodRedisplayFilters(value.filters)
    }

    override fun configure(
        value: RedisplayFilterType,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        customerEphemeralKeyRequestBuilder.paymentMethodRedisplayFilters(value.filters)
    }

    enum class RedisplayFilterType(
        override val value: String,
        val filters: List<AllowRedisplayFilter>
    ) : ValueEnum {
        Always("always", listOf(AllowRedisplayFilter.Always)),
        Limited("limited", listOf(AllowRedisplayFilter.Limited)),
        Unspecified("unspecified", listOf(AllowRedisplayFilter.Unspecified)),
        All(
            "all",
            listOf(
                AllowRedisplayFilter.Always,
                AllowRedisplayFilter.Limited,
                AllowRedisplayFilter.Unspecified
            )
        ),
        None("none", emptyList()),
    }
}

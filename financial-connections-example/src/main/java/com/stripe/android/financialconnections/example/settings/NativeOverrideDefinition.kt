package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.NativeOverride
import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal class NativeOverrideDefinition :
    PlaygroundSettingDefinition.Saveable<NativeOverride>, PlaygroundSettingDefinition.Displayable<NativeOverride> {

    override val displayName: String = "Native Override"
    override val options: List<Option<NativeOverride>>
        get() = listOf(
            option("None", NativeOverride.None),
            option("Native", NativeOverride.Native),
            option("Web", NativeOverride.Web),
        )

    /**
     * Not used in create session request.
     */
    override fun sessionRequest(
        body: LinkAccountSessionBody,
        value: Any?
    ): LinkAccountSessionBody = body

    override val key: String = "financial_connections_override_native"
    override val defaultValue: NativeOverride = NativeOverride.None

    override fun convertToValue(value: String): NativeOverride = NativeOverride.valueOf(value)

    override fun convertToString(value: NativeOverride): String = value.name
}
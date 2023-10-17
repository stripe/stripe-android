package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal class EmailDefinition : PlaygroundSettingDefinition.Displayable<String> {
    override val displayName: String
        get() = "Customer email"
    override val options: List<Option<String>> = emptyList()

    override fun sessionRequest(
        body: LinkAccountSessionBody,
        value: Any?
    ): LinkAccountSessionBody = body.copy(
        customerEmail = value as String?
    )
}
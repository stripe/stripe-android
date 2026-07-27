@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.paymentsheet.example.playground.settings

import androidx.compose.ui.graphics.Color
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object LinkControllerCustomAppearanceSettingsDefinition : BooleanSettingsDefinition(
    key = "linkControllerCustomAppearance",
    displayName = "LinkController: custom appearance",
    defaultValue = false,
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.LinkController
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: LinkController.Configuration,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData
    ) {
        if (value) {
            val purple = Color(0xFF6B4EFF)
            configurationBuilder.appearance(
                LinkAppearance()
                    .lightColors(
                        LinkAppearance.Colors()
                            .primary(purple)
                            .contentOnPrimary(Color.White)
                            .borderSelected(purple)
                    )
                    .darkColors(
                        LinkAppearance.Colors()
                            .primary(purple)
                            .contentOnPrimary(Color.White)
                            .borderSelected(purple)
                    )
                    .style(LinkAppearance.Style.ALWAYS_DARK)
                    .primaryButton(
                        LinkAppearance.PrimaryButton()
                            .cornerRadiusDp(6f)
                    )
            )
        }
    }
}

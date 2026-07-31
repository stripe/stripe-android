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
    private val customPrimaryColor = Color(0xFF6B4EFF)
    private const val primaryButtonCornerRadiusDp = 6f

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
            configurationBuilder.appearance(
                LinkAppearance()
                    .lightColors(
                        LinkAppearance.Colors()
                            .primary(customPrimaryColor)
                            .contentOnPrimary(Color.White)
                            .borderSelected(customPrimaryColor)
                    )
                    .darkColors(
                        LinkAppearance.Colors()
                            .primary(customPrimaryColor)
                            .contentOnPrimary(Color.White)
                            .borderSelected(customPrimaryColor)
                    )
                    .style(LinkAppearance.Style.ALWAYS_DARK)
                    .primaryButton(
                        LinkAppearance.PrimaryButton()
                            .cornerRadiusDp(primaryButtonCornerRadiusDp)
                    )
            )
        }
    }
}

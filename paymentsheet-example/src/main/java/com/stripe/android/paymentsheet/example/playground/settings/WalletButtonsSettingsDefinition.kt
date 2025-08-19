package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object WalletButtonsSettingsDefinition :
    PlaygroundSettingDefinition<WalletButtonsPlaygroundType>,
    PlaygroundSettingDefinition.Saveable<WalletButtonsPlaygroundType> by EnumSaveable(
        key = "walletButtons",
        values = WalletButtonsPlaygroundType.entries.toTypedArray(),
        defaultValue = WalletButtonsPlaygroundType.Disabled,
    ),
    PlaygroundSettingDefinition.Displayable<WalletButtonsPlaygroundType> {
    override val displayName: String = "Wallet Buttons"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = WalletButtonsPlaygroundType.entries.map {
        option(it.displayName, it)
    }

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return when (configurationData.integrationType) {
            PlaygroundConfigurationData.IntegrationType.Embedded,
            PlaygroundConfigurationData.IntegrationType.FlowController,
            PlaygroundConfigurationData.IntegrationType.FlowControllerWithSpt -> true
            PlaygroundConfigurationData.IntegrationType.PaymentSheet,
            PlaygroundConfigurationData.IntegrationType.CustomerSheet,
            PlaygroundConfigurationData.IntegrationType.LinkController -> false
        }
    }

    override fun configure(
        value: WalletButtonsPlaygroundType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configureWalletButtons(value, configurationBuilder)
    }

    override fun configure(
        value: WalletButtonsPlaygroundType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configureWalletButtons(value, configurationBuilder)
    }

    @OptIn(WalletButtonsPreview::class)
    private fun configureWalletButtons(
        value: WalletButtonsPlaygroundType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
    ) {
        val configuration = when (value) {
            WalletButtonsPlaygroundType.Disabled -> {
                PaymentSheet.WalletButtonsConfiguration(
                    willDisplayExternally = false
                )
            }
            WalletButtonsPlaygroundType.Enabled -> {
                PaymentSheet.WalletButtonsConfiguration(
                    willDisplayExternally = true
                )
            }
            WalletButtonsPlaygroundType.EnabledWithOnlyLink -> {
                PaymentSheet.WalletButtonsConfiguration(
                    willDisplayExternally = true,
                    walletsToShow = listOf("link")
                )
            }
        }

        configurationBuilder.walletButtons(configuration)
    }
}

enum class WalletButtonsPlaygroundType(
    val displayName: String
) : ValueEnum {
    Disabled("Disabled"),
    Enabled("Enabled"),
    EnabledWithOnlyLink("Enabled w/ only Link");

    override val value: String
        get() = name
}

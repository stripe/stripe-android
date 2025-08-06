package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.elements.payment.WalletButtonsConfiguration
import com.stripe.android.paymentelement.WalletButtonsPreview
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

    override fun configure(
        value: WalletButtonsPlaygroundType,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configureWalletButtons(value, configurationBuilder)
    }

    override fun configure(
        value: WalletButtonsPlaygroundType,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configureWalletButtons(value, configurationBuilder)
    }

    @OptIn(WalletButtonsPreview::class)
    private fun configureWalletButtons(
        value: WalletButtonsPlaygroundType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
    ) {
        configureWalletButtonsHelper(value) {
            configurationBuilder.walletButtons(it)
        }
    }

    @OptIn(WalletButtonsPreview::class)
    private fun configureWalletButtons(
        value: WalletButtonsPlaygroundType,
        configurationBuilder: FlowController.Configuration.Builder,
    ) {
        configureWalletButtonsHelper(value) {
            configurationBuilder.walletButtons(it)
        }
    }

    @OptIn(WalletButtonsPreview::class)
    private fun configureWalletButtonsHelper(
        value: WalletButtonsPlaygroundType,
        configurationBuilderBlock: (WalletButtonsConfiguration) -> Unit,
    ) {
        val configuration = when (value) {
            WalletButtonsPlaygroundType.Disabled -> {
                WalletButtonsConfiguration(
                    willDisplayExternally = false
                )
            }
            WalletButtonsPlaygroundType.Enabled -> {
                WalletButtonsConfiguration(
                    willDisplayExternally = true
                )
            }
            WalletButtonsPlaygroundType.EnabledWithOnlyLink -> {
                WalletButtonsConfiguration(
                    willDisplayExternally = true,
                    walletsToShow = listOf("link")
                )
            }
        }

        configurationBuilderBlock(configuration)
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

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
        configurationBuilder.walletButtons(value.configuration)
    }
}

enum class WalletButtonsPlaygroundType(
    val displayName: String,
    val configuration: PaymentSheet.WalletButtonsConfiguration,
) : ValueEnum {
    Disabled(
        displayName = "Disabled",
        configuration = PaymentSheet.WalletButtonsConfiguration(
            willDisplayExternally = false,
        ),
    ),
    Automatic(
        displayName = "Automatic",
        configuration = PaymentSheet.WalletButtonsConfiguration(
            willDisplayExternally = true,
        ),
    ),
    Both(
        displayName = "Always in MPE & Wallets",
        configuration = PaymentSheet.WalletButtonsConfiguration(
            willDisplayExternally = true,
            visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                paymentElement = mapOf(
                    PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                        PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Always,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                        PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Always,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                        PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Always,
                ),
                walletButtonsView = mapOf(
                    PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                ),
            )
        ),
    ),
    GPayAlwaysLinkAutoNeverShopPayAuto(
        displayName = "Google Pay (Always), Link (Never in Wallets, Auto in MPE), Shop Pay (Automatic)",
        configuration = PaymentSheet.WalletButtonsConfiguration(
            willDisplayExternally = true,
            visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                paymentElement = mapOf(
                    PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                        PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Always,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                        PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Automatic,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                        PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Automatic,
                ),
                walletButtonsView = mapOf(
                    PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never,
                    PaymentSheet.WalletButtonsConfiguration.Wallet.ShopPay to
                        PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always,
                ),
            )
        ),
    );

    override val value: String
        get() = name
}

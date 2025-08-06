package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object CardBrandAcceptanceSettingsDefinition :
    PlaygroundSettingDefinition<CardBrandAcceptanceType>,
    PlaygroundSettingDefinition.Saveable<CardBrandAcceptanceType>,
    PlaygroundSettingDefinition.Displayable<CardBrandAcceptanceType> {

    override val displayName: String = "Card Brand Acceptance"

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow() ||
            configurationData.integrationType.isCustomerFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<CardBrandAcceptanceType>> {
        return listOf(
            option("All", CardBrandAcceptanceType.All),
            option("Disallow Visa", CardBrandAcceptanceType.DisallowVisa),
            option("Allow Visa", CardBrandAcceptanceType.AllowVisa)
        )
    }

    override fun configure(
        value: CardBrandAcceptanceType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationBuilder.cardBrandAcceptance(value.cardBrandAcceptance)
    }

    override fun configure(
        value: CardBrandAcceptanceType,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configurationBuilder.cardBrandAcceptance(value.cardBrandAcceptance)
    }

    override fun configure(
        value: CardBrandAcceptanceType,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.cardBrandAcceptance(value.cardBrandAcceptance)
    }

    override fun configure(
        value: CardBrandAcceptanceType,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData
    ) {
        configurationBuilder.cardBrandAcceptance(value.cardBrandAcceptance)
    }

    override fun configure(
        value: CardBrandAcceptanceType,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationBuilder.cardBrandAcceptance(value.cardBrandAcceptance)
    }

    override val key: String = "card_brand_acceptance"
    override val defaultValue: CardBrandAcceptanceType = CardBrandAcceptanceType.All

    override fun convertToValue(value: String): CardBrandAcceptanceType {
        return when (value) {
            CardBrandAcceptanceType.All.value -> CardBrandAcceptanceType.All
            CardBrandAcceptanceType.DisallowVisa.value -> CardBrandAcceptanceType.DisallowVisa
            CardBrandAcceptanceType.AllowVisa.value -> CardBrandAcceptanceType.AllowVisa
            else -> defaultValue
        }
    }

    override fun convertToString(value: CardBrandAcceptanceType): String {
        return value.value
    }
}

sealed class CardBrandAcceptanceType(val value: String, val cardBrandAcceptance: CardBrandAcceptance) {
    object All : CardBrandAcceptanceType("all", CardBrandAcceptance.all())

    object DisallowVisa : CardBrandAcceptanceType(
        "disallow_visa",
        CardBrandAcceptance.disallowed(
            brands = listOf(CardBrandAcceptance.BrandCategory.Visa)
        )
    )

    object AllowVisa : CardBrandAcceptanceType(
        "allow_visa",
        CardBrandAcceptance.allowed(
            brands = listOf(CardBrandAcceptance.BrandCategory.Visa)
        )
    )
}

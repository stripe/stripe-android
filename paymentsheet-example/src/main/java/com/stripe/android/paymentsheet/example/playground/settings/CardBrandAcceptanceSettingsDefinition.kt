@file:OptIn(ExperimentalCardBrandFilteringApi::class)

package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.ExperimentalCardBrandFilteringApi
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object CardBrandAcceptanceSettingsDefinition :
    PlaygroundSettingDefinition<CardBrandAcceptanceType>,
    PlaygroundSettingDefinition.Saveable<CardBrandAcceptanceType>,
    PlaygroundSettingDefinition.Displayable<CardBrandAcceptanceType> {

    override val displayName: String = "Card Brand Acceptance"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<CardBrandAcceptanceType>> {
        return listOf(
            option("All", CardBrandAcceptanceType.All),
            option("Disallow Amex", CardBrandAcceptanceType.DisallowAmex),
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
            CardBrandAcceptanceType.DisallowAmex.value -> CardBrandAcceptanceType.DisallowAmex
            CardBrandAcceptanceType.AllowVisa.value -> CardBrandAcceptanceType.AllowVisa
            else -> defaultValue
        }
    }

    override fun convertToString(value: CardBrandAcceptanceType): String {
        return value.value
    }
}

sealed class CardBrandAcceptanceType(val value: String, val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance) {
    object All : CardBrandAcceptanceType("all", PaymentSheet.CardBrandAcceptance.all())

    object DisallowAmex : CardBrandAcceptanceType(
        "disallow_amex",
        PaymentSheet.CardBrandAcceptance.disallowed(
            brands = listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex)
        )
    )

    object AllowVisa : CardBrandAcceptanceType(
        "allow_visa",
        PaymentSheet.CardBrandAcceptance.allowed(
            brands = listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
        )
    )
}

package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

@OptIn(CardFundingFilteringPrivatePreview::class)
internal object CardFundingAcceptanceSettingsDefinition :
    PlaygroundSettingDefinition<CardFundingAcceptanceType>,
    PlaygroundSettingDefinition.Saveable<CardFundingAcceptanceType>,
    PlaygroundSettingDefinition.Displayable<CardFundingAcceptanceType> {
    override val defaultValue = CardFundingAcceptanceType.All
    override val key = "card_funding_acceptance"
    override val displayName = "Card Funding Acceptance"

    override fun convertToString(value: CardFundingAcceptanceType) = value.value

    override fun convertToValue(value: String): CardFundingAcceptanceType {
        return when (value) {
            CardFundingAcceptanceType.All.value -> CardFundingAcceptanceType.All
            CardFundingAcceptanceType.CreditOnly.value -> CardFundingAcceptanceType.CreditOnly
            CardFundingAcceptanceType.DebitOnly.value -> CardFundingAcceptanceType.DebitOnly
            else -> defaultValue
        }
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<CardFundingAcceptanceType>> {
        return listOf(
            option("All", CardFundingAcceptanceType.All),
            option("Credit Only", CardFundingAcceptanceType.CreditOnly),
            option("Debit Only", CardFundingAcceptanceType.DebitOnly),
        )
    }

    override fun configure(
        value: CardFundingAcceptanceType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.allowedCardFundingTypes(value.cardFundingTypes)
    }

    override fun configure(
        value: CardFundingAcceptanceType,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.allowedCardFundingTypes(value.cardFundingTypes)
    }
}

sealed class CardFundingAcceptanceType(
    val value: String,
    val cardFundingTypes: List<PaymentSheet.CardFundingType>
) {
    object All : CardFundingAcceptanceType(
        value = "all",
        cardFundingTypes = PaymentSheet.CardFundingType.entries
    )

    object CreditOnly : CardFundingAcceptanceType(
        value = "creditOnly",
        cardFundingTypes = listOf(PaymentSheet.CardFundingType.Credit)
    )

    object DebitOnly : CardFundingAcceptanceType(
        value = "debitOnly",
        cardFundingTypes = listOf(PaymentSheet.CardFundingType.Debit)
    )
}

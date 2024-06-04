package com.stripe.android.ui.core.elements

import com.stripe.android.model.CardBrand

internal sealed interface CardBrandChoiceConfig {
    data class Eligible(
        val preferredBrands: List<CardBrand>,
        val initialBrand: CardBrand?
    ) : CardBrandChoiceConfig

    object Ineligible : CardBrandChoiceConfig
}

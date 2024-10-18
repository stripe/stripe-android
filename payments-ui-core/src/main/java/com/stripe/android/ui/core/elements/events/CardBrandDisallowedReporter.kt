package com.stripe.android.ui.core.elements.events

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CardBrandDisallowedReporter {
    fun onDisallowedCardBrandEntered(brand: CardBrand): Unit
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalCardBrandDisallowedReporter = staticCompositionLocalOf<CardBrandDisallowedReporter> {
    EmptyCardBrandDisallowedReporter
}

private object EmptyCardBrandDisallowedReporter : CardBrandDisallowedReporter {
    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
        if (BuildConfig.DEBUG) {
            error("CardBrandDisallowedReporter.onDisallowedCardBrandEntered() was not reported")
        }
    }
}

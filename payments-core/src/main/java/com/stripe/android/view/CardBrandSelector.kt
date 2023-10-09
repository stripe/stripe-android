@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.view

import androidx.annotation.RestrictTo
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown

fun selectCardBrandToDisplay(
    userSelectedBrand: CardBrand?,
    possibleBrands: List<CardBrand>,
    merchantPreferredBrands: List<CardBrand>,
): CardBrand {
    val userChoice = userSelectedBrand.takeIf { it == Unknown || it in possibleBrands }
    val merchantChoice = merchantPreferredBrands.firstOrNull { it in possibleBrands }
    return userChoice ?: merchantChoice ?: Unknown
}

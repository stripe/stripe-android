package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.R
import com.stripe.android.uicore.utils.asIndividualDigits

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal fun formatCardNumberInputForAccessibility(
    input: String,
    preferredBrands: List<CardBrand>,
): ResolvableString {
    if (input.isNotEmpty() || preferredBrands.isEmpty()) {
        return input.asIndividualDigits().resolvableString
    }

    return resolvableString(
        R.string.stripe_card_number_with_brands_content_description,
        preferredBrands.joinToString(", ") { it.displayName }
    )
}
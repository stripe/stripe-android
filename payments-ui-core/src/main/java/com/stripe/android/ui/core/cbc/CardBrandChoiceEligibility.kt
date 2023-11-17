package com.stripe.android.ui.core.cbc

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.CardBrand
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface CardBrandChoiceEligibility : Parcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Eligible(val preferredNetworks: List<CardBrand>) : CardBrandChoiceEligibility

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    object Ineligible : CardBrandChoiceEligibility
}

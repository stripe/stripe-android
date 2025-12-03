package com.stripe.android.paymentsheet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Card funding categories that can be filtered.
 */
@Parcelize
internal data class CardFundingAcceptance(
    val cardFundingTypes: List<PaymentSheet.CardFundingType> = PaymentSheet.CardFundingType.entries
) : Parcelable

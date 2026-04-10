package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardArtMetadata(
    val cardArts: List<PaymentMethod.Card.CardArt>,
) : Parcelable

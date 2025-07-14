package com.stripe.android.model

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class IssuingCardPin(
    val pin: String
) : Parcelable

package com.stripe.android.link.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkAuthIntentInfo(
    val linkAuthIntentId: String,
    val consentPresentation: ConsentPresentation?,
) : Parcelable

package com.stripe.android.lpmfoundations.paymentmethod.link

import android.os.Parcelable
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.inline.LinkSignupMode
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkInlineConfiguration(
    val signupMode: LinkSignupMode,
    val linkConfiguration: LinkConfiguration,
) : Parcelable

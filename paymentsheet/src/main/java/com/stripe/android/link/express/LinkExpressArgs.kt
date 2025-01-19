package com.stripe.android.link.express

import android.os.Parcelable
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkExpressArgs(
    val configuration: LinkConfiguration,
    val publishableKey: String,
    val stripeAccountId: String?,
    val linkAccount: LinkAccount?
) : Parcelable

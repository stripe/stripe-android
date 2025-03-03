package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.link.model.LinkAccount
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class NativeLinkArgs(
    val configuration: LinkConfiguration,
    val publishableKey: String,
    val stripeAccountId: String?,
    val startWithVerificationDialog: Boolean,
    val linkAccount: LinkAccount?,
    val instanceId: String,
) : Parcelable

package com.stripe.android.link

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class NativeLinkArgs(
    val configuration: LinkConfiguration,
    val publishableKey: String,
    val stripeAccountId: String?,
    val startWithVerificationDialog: Boolean,
    val linkAccountInfo: LinkAccountUpdate.Value,
    val paymentElementCallbackIdentifier: String,
    val launchMode: LinkLaunchMode,
) : Parcelable

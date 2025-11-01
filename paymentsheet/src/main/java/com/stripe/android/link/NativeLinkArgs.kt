package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.RequestSurface
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class NativeLinkArgs(
    val configuration: LinkConfiguration,
    val paymentMethodMetadata: PaymentMethodMetadata,
    val requestSurface: RequestSurface,
    val publishableKey: String,
    val stripeAccountId: String?,
    val linkExpressMode: LinkExpressMode,
    val linkAccountInfo: LinkAccountUpdate.Value,
    val paymentElementCallbackIdentifier: String,
    val launchMode: LinkLaunchMode,
) : Parcelable

package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.RequestSurface
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class NativeLinkArgs(
    val configuration: LinkConfiguration,
    val paymentMethodMetadata: PaymentMethodMetadata,
    val requestSurface: RequestSurface,
    val apiConfiguration: ApiConfiguration.State,
    val linkExpressMode: LinkExpressMode,
    val linkAccountInfo: LinkAccountUpdate.Value,
    val paymentElementCallbackIdentifier: String,
    val launchMode: LinkLaunchMode,
    val statusBarColor: Int?,
) : Parcelable

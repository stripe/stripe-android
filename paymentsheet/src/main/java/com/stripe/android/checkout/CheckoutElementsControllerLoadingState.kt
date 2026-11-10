package com.stripe.android.checkout

import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize

@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class CheckoutElementsControllerLoadingState(
    val configuration: CheckoutElementsController.Configuration.State,
) : Parcelable

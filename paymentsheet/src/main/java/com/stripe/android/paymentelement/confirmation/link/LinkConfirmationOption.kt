package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkConfirmationOption(
    val configuration: LinkConfiguration,
) : ConfirmationHandler.Option

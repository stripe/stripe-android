package com.stripe.android.paymentelement.confirmation.linkexpress

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkExpressConfirmationOption(
    val configuration: LinkConfiguration,
    val linkAccount: LinkAccount
) : ConfirmationHandler.Option

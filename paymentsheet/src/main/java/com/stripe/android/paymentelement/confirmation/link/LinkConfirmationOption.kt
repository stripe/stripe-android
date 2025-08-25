package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkConfirmationOption(
    val configuration: LinkConfiguration,
    val linkLaunchMode: LinkLaunchMode = LinkLaunchMode.Full,
    val linkExpressMode: LinkExpressMode,
    val passiveCaptchaParams: PassiveCaptchaParams?
) : ConfirmationHandler.Option

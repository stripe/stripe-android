package com.stripe.android.common.spms

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import javax.inject.Inject

internal interface LinkInlineSignupAvailability {
    sealed interface Result {
        data class Available(val configuration: LinkConfiguration) : Result

        data object Unavailable : Result
    }

    fun availability(): Result
}

internal class DefaultLinkInlineSignupAvailability @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
) : LinkInlineSignupAvailability {
    override fun availability(): LinkInlineSignupAvailability.Result {
        val linkState = paymentMethodMetadata.linkState

        return when (linkState?.signupMode) {
            null -> LinkInlineSignupAvailability.Result.Unavailable
            else -> LinkInlineSignupAvailability.Result.Available(
                configuration = linkState.configuration,
            )
        }
    }
}

package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal fun confirmationStateConfirming(selection: PaymentSelection): ConfirmationHandler.State.Confirming {
    val confirmationOption = selection.toConfirmationOption(
        configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration.asCommonConfiguration(),
        linkConfiguration = null
    )
    return ConfirmationHandler.State.Confirming(requireNotNull(confirmationOption))
}

internal fun confirmationStateComplete(succeeded: Boolean): ConfirmationHandler.State.Complete {
    val result = if (succeeded) {
        ConfirmationHandler.Result.Succeeded(
            PaymentIntentFixtures.PI_SUCCEEDED,
            null
        )
    } else {
        ConfirmationHandler.Result.Failed(
            cause = Throwable(),
            message = "Something went wrong".resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.Internal
        )
    }
    return ConfirmationHandler.State.Complete(result)
}

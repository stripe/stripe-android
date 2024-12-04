package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.common.ui.UpdateExternalPaymentMethodConfirmHandler
import com.stripe.android.common.ui.UpdateIntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler

/**
 * Creates an [EmbeddedPaymentElement] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param createIntentCallback Called when the customer confirms the payment or setup.
 * @param externalPaymentMethodConfirmHandler Called when a user confirms payment for an external payment method.
 * @param resultCallback Called with the result of the payment.
 */
@ExperimentalEmbeddedPaymentElementApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberEmbeddedPaymentElement(
    createIntentCallback: CreateIntentCallback,
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null,
    resultCallback: EmbeddedPaymentElement.ResultCallback,
): EmbeddedPaymentElement {
    val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current) {
        "EmbeddedPaymentElement must have a ViewModelStoreOwner."
    }

    UpdateExternalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
    UpdateIntentConfirmationInterceptor(createIntentCallback)

    val onResult by rememberUpdatedState(newValue = resultCallback::onResult)

    return remember {
        EmbeddedPaymentElement.create(
            viewModelStoreOwner = viewModelStoreOwner,
            resultCallback = onResult,
        )
    }
}

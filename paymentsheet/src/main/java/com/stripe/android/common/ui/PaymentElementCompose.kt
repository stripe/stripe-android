package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor

@Composable
internal fun UpdateIntentConfirmationInterceptor(
    createIntentCallback: CreateIntentCallback?,
) {
    LaunchedEffect(createIntentCallback) {
        IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
    }
}

@Composable
internal fun UpdateExternalPaymentMethodConfirmHandler(
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
) {
    LaunchedEffect(externalPaymentMethodConfirmHandler) {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
    }
}

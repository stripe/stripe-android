package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun RevealPrimaryButtonWhenEnabled(
    isEnabled: Boolean,
    isImeVisible: Boolean,
    bringIntoViewRequester: BringIntoViewRequester,
) {
    LaunchedEffect(isEnabled, isImeVisible) {
        if (isEnabled && isImeVisible) {
            bringIntoViewRequester.bringIntoView()
        }
    }
}

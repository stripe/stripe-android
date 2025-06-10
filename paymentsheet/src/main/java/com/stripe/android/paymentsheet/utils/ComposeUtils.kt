package com.stripe.android.paymentsheet.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.StripeTheme

@Composable
internal fun PaymentSheetContentPadding(subtractingExtraPadding: Dp = 0.dp) {
    val bottomPadding = StripeTheme.formInsets.bottom.dp - subtractingExtraPadding
    Spacer(modifier = Modifier.requiredHeight(bottomPadding.coerceAtLeast(0.dp)))
}

@Composable
internal fun DismissKeyboardOnProcessing(processing: Boolean) {
    val keyboardController = LocalSoftwareKeyboardController.current

    if (processing) {
        LaunchedEffect(Unit) {
            keyboardController?.hide()
        }
    }
}

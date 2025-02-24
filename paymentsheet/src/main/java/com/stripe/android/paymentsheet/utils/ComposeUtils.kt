package com.stripe.android.paymentsheet.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

@Composable
internal fun PaymentSheetContentPadding(subtractingExtraPadding: Dp = 0.dp) {
    val bottomPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing_bottom)
    Spacer(modifier = Modifier.requiredHeight(bottomPadding - subtractingExtraPadding))
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

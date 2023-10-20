package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

@Composable
internal fun EditPaymentMethod(
    sheetViewModel: BaseSheetViewModel,
    paymentMethod: PaymentMethod,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Editing ${paymentMethod.getLabel(LocalContext.current.resources)}",
        modifier = Modifier.padding(16.dp),
    )
}

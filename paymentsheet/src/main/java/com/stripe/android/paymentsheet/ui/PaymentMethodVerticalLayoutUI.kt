package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.uicore.image.StripeImageLoader

@Composable
internal fun PaymentMethodVerticalLayoutUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { onViewMorePaymentMethods() }) {
            Text(text = "Go to manage screen")
        }

        NewPaymentMethodVerticalLayoutUI(
            paymentMethods = paymentMethods,
            selectedIndex = selectedIndex,
            isEnabled = isEnabled,
            onItemSelectedListener = onItemSelectedListener,
            imageLoader = imageLoader
        )
    }
}

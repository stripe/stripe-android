package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.uicore.image.StripeImageLoader

internal const val TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI = "TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI"

@Composable
internal fun NewPaymentMethodVerticalLayoutUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        paymentMethods.forEachIndexed { index, item ->
            NewPaymentMethodRowButton(
                isEnabled = isEnabled,
                isSelected = index == selectedIndex,
                supportedPaymentMethod = item,
                imageLoader = imageLoader,
                onClick = onItemSelectedListener
            )
        }
    }
}

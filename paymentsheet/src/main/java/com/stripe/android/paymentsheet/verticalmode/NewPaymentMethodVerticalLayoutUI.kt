package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.uicore.image.StripeImageLoader

internal const val TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI = "TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI"

@Composable
internal fun NewPaymentMethodVerticalLayoutUI(
    paymentMethods: List<DisplayablePaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
    reportInitialPaymentMethodVisibilitySnapshot: (Map<String, Boolean>) -> Unit = {},
    rowStyle: Embedded.RowStyle = Embedded.RowStyle.FloatingButton.default
) {
    val paymentMethodCodes = remember(paymentMethods) { paymentMethods.map { it.code } }
    val visibilityTracker = remember(paymentMethodCodes) {
        PaymentMethodInitialVisibilityTracker(
            expectedItems = paymentMethodCodes,
            renderedLpmCallback = reportInitialPaymentMethodVisibilitySnapshot,
        )
    }

    DisposableEffect(visibilityTracker) {
        onDispose {
            // Cancel any running coroutines when UI is disposed
            visibilityTracker.dispose()
        }
    }

    Column(
        modifier = modifier.testTag(TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        paymentMethods.forEachIndexed { index, item ->
            NewPaymentMethodRowButton(
                isEnabled = isEnabled,
                isSelected = index == selectedIndex,
                displayablePaymentMethod = item,
                imageLoader = imageLoader,
                appearance = Embedded.Builder().rowStyle(rowStyle).build(),
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    visibilityTracker.updateVisibility(item.code, coordinates)
                },
            )
        }
    }
}

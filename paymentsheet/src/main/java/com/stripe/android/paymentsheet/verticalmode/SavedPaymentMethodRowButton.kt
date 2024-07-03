package com.stripe.android.paymentsheet.verticalmode

import android.content.res.Resources
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ui.PaymentMethodIconFromResource
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon

@Composable
internal fun SavedPaymentMethodRowButton(
    displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    resources: Resources?,
    isEnabled: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val paymentMethodTitle =
        resources?.let { displayableSavedPaymentMethod.paymentMethod.getLabel(resources) }
            ?: displayableSavedPaymentMethod.displayName

    PaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        iconContent = {
            PaymentMethodIconFromResource(
                iconRes = displayableSavedPaymentMethod.paymentMethod.getSavedPaymentMethodIcon(forVerticalMode = true),
                colorFilter = null,
                modifier = Modifier.padding(4.dp).height(16.dp).width(24.dp)
            )
        },
        title = paymentMethodTitle,
        subtitle = null,
        onClick = onClick,
        modifier = modifier.testTag(
            "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${displayableSavedPaymentMethod.paymentMethod.id}"
        ),
        trailingContent = trailingContent,
    )
}

@Preview
@Composable
internal fun PreviewCardSavedPaymentMethodRowButton() {
    val cardSavedPaymentMethod = DisplayableSavedPaymentMethod(
        displayName = "4242",
        paymentMethod = PaymentMethod(
            id = "001",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.Card.code,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = "4242",
            )
        )
    )

    SavedPaymentMethodRowButton(
        displayableSavedPaymentMethod = cardSavedPaymentMethod,
        resources = LocalContext.current.resources,
        isEnabled = true,
        isSelected = true,
    )
}

internal const val TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON = "saved_payment_method_row_button"

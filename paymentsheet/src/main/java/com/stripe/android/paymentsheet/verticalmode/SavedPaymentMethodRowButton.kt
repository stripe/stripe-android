package com.stripe.android.paymentsheet.verticalmode

import android.content.res.Resources
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
            )
        },
        title = paymentMethodTitle,
        subtitle = null,
        onClick = {},
        modifier = modifier,
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

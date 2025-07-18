package com.stripe.android.paymentsheet.verticalmode

import android.content.res.Configuration
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ui.PaymentMethodIconFromResource
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.paymentsheet.ui.getSublabel
import com.stripe.android.paymentsheet.ui.readNumbersAsIndividualDigits
import com.stripe.android.paymentsheet.utils.testMetadata
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconHeight
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconWidth
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun SavedPaymentMethodRowButton(
    displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    isEnabled: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    appearance: Embedded = Embedded.default,
    disclosureView: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val contentDescription = displayableSavedPaymentMethod
        .getDescription()
        .resolve()
        .readNumbersAsIndividualDigits()
    val paymentMethodTitle =
        displayableSavedPaymentMethod.paymentMethod.getLabel(canShowSublabel = true)
            ?: displayableSavedPaymentMethod.displayName

    val paymentMethodId = displayableSavedPaymentMethod.paymentMethod.id
    PaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        iconContent = {
            val displayBrand = displayableSavedPaymentMethod.paymentMethod.card?.displayBrand
            PaymentMethodIconFromResource(
                iconRes = displayableSavedPaymentMethod.paymentMethod.getSavedPaymentMethodIcon(forVerticalMode = true),
                colorFilter = null,
                alignment = Alignment.Center,
                modifier = Modifier
                    .height(iconHeight)
                    .width(iconWidth)
                    .testMetadata(displayBrand)
            )
        },
        title = paymentMethodTitle.resolve(),
        subtitle = displayableSavedPaymentMethod.paymentMethod.getSublabel()?.resolve(),
        promoText = null,
        onClick = onClick,
        modifier = modifier
            .testTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId"
            ),
        contentDescription = contentDescription,
        trailingContent = trailingContent,
        appearance = appearance,
        shouldShowDefaultBadge = displayableSavedPaymentMethod.shouldShowDefaultBadge,
        disclosureView = disclosureView
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
internal fun PreviewCardSavedPaymentMethodRowButton() {
    val cardSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
        displayName = "4242".resolvableString,
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

    DefaultStripeTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = cardSavedPaymentMethod,
                isEnabled = true,
                isSelected = true,
            )
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = cardSavedPaymentMethod,
                isEnabled = false,
                isSelected = false,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
internal fun PreviewCardDefaultSavedPaymentMethodRowButton() {
    val defaultSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
        displayName = "4242".resolvableString,
        shouldShowDefaultBadge = true,
        paymentMethod = PaymentMethod(
            id = "002",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.Card.code,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.AmericanExpress,
                last4 = "4444",
            )
        )
    )

    DefaultStripeTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = defaultSavedPaymentMethod,
                isEnabled = true,
                isSelected = true,
            )
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = defaultSavedPaymentMethod,
                isEnabled = false,
                isSelected = false,
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON = "saved_payment_method_row_button"

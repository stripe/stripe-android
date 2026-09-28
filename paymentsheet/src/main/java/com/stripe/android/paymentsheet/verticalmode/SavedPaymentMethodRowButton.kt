package com.stripe.android.paymentsheet.verticalmode

import android.content.res.Configuration
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ui.CardArtImage
import com.stripe.android.paymentsheet.ui.PaymentMethodIconFromResource
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.paymentsheet.ui.getSublabel
import com.stripe.android.paymentsheet.ui.readNumbersAsIndividualDigits
import com.stripe.android.paymentsheet.utils.testMetadata
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconHeight
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.strings.resolve

internal const val SAVED_PAYMENT_METHOD_PENDING_TEST_TAG = "embedded_saved_payment_method_pending"

@Composable
internal fun SavedPaymentMethodRowButton(
    displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    linkBrand: LinkBrand,
    isEnabled: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    appearance: Embedded = Embedded(Embedded.RowStyle.FloatingButton.default),
    onClick: () -> Unit = {},
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val contentDescription = displayableSavedPaymentMethod
        .getDescription()
        .resolve()
        .readNumbersAsIndividualDigits()
    val paymentMethodTitle =
        displayableSavedPaymentMethod.paymentMethod.getLabel(
            linkBrand = linkBrand,
            canShowSublabel = true,
        ) ?: displayableSavedPaymentMethod.displayName

    val paymentMethodId = displayableSavedPaymentMethod.paymentMethod.id
    PaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        iconContent = {
            SavedPaymentMethodIcon(
                displayableSavedPaymentMethod = displayableSavedPaymentMethod,
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
        promotionProvider = null
    )
}

@Composable
private fun SavedPaymentMethodIcon(
    displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
) {
    Box(
        modifier = Modifier
            .width(UIConstants.iconWidth)
            .height(iconHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (displayableSavedPaymentMethod.isSelectionPending) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(iconHeight)
                    .testTag(SAVED_PAYMENT_METHOD_PENDING_TEST_TAG),
            )
        } else {
            val paymentMethod = displayableSavedPaymentMethod.paymentMethod
            CardArtImage(
                url = paymentMethod.card?.cardArt?.artImage?.url,
                modifier = Modifier
                    .width(UIConstants.iconWidth)
                    .height(iconHeight)
            ) {
                PaymentMethodIconFromResource(
                    iconRes = paymentMethod.getSavedPaymentMethodIcon(
                        // Link brand doesn't matter in vertical mode, where we only show the icon.
                        linkBrand = LinkBrand.Link,
                        forVerticalMode = true
                    ),
                    colorFilter = null,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .height(iconHeight)
                        .width(UIConstants.iconWidth)
                        .testMetadata(paymentMethod.card?.displayBrand)
                )
            }
        }
    }
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
        ),
    )

    DefaultStripeTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = cardSavedPaymentMethod,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = true,
            )
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = cardSavedPaymentMethod,
                linkBrand = LinkBrand.Link,
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
        ),
    )

    DefaultStripeTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = defaultSavedPaymentMethod,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = true,
            )
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = defaultSavedPaymentMethod,
                linkBrand = LinkBrand.Link,
                isEnabled = false,
                isSelected = false,
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON = "saved_payment_method_row_button"

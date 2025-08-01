package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconHeight
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconWidth
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON = "TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON"

@Composable
internal fun NewPaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    displayablePaymentMethod: DisplayablePaymentMethod,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    appearance: Embedded = Embedded(Embedded.RowStyle.FloatingButton.default)
) {
    NewPaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        iconRes = displayablePaymentMethod.icon(),
        iconUrl = displayablePaymentMethod.iconUrl(),
        imageLoader = imageLoader,
        title = displayablePaymentMethod.displayName.resolve(),
        subtitle = displayablePaymentMethod.subtitle?.resolve(),
        promoText = displayablePaymentMethod.promoBadge,
        iconRequiresTinting = displayablePaymentMethod.iconRequiresTinting,
        onClick = {
            displayablePaymentMethod.onClick()
        },
        modifier = modifier.testTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_${displayablePaymentMethod.code}"),
        appearance = appearance,
        trailingContent = trailingContent,
    )
}

@Composable
internal fun NewPaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    title: String,
    subtitle: String?,
    promoText: String?,
    iconRequiresTinting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    appearance: Embedded = Embedded(Embedded.RowStyle.FloatingButton.default)
) {
    PaymentMethodRowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        shouldShowDefaultBadge = false,
        iconContent = {
            PaymentMethodIcon(
                iconRes = iconRes,
                iconUrl = iconUrl,
                imageLoader = imageLoader,
                iconRequiresTinting = iconRequiresTinting,
                modifier = modifier.height(iconHeight).width(iconWidth),
                contentAlignment = Alignment.Center,
            )
        },
        title = title,
        subtitle = subtitle,
        promoText = promoText,
        onClick = onClick,
        modifier = modifier,
        appearance = appearance,
        trailingContent = trailingContent,
    )
}

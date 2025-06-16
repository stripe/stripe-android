package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.DrawableRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.uicore.IconStyle

internal data class DisplayablePaymentMethod(
    val code: PaymentMethodCode,
    val syntheticCode: String = code,
    val displayName: ResolvableString,
    @DrawableRes private val iconResource: Int,
    val lightThemeIconUrl: String?,
    val darkThemeIconUrl: String?,
    val iconRequiresTinting: Boolean,
    val subtitle: ResolvableString? = null,
    val promoBadge: String? = null,
    val onClick: () -> Unit,
    @DrawableRes private val outlinedIconResource: Int? = null,
) {
    fun icon(style: IconStyle) = when (style) {
        IconStyle.Filled -> iconResource
        IconStyle.Outlined -> outlinedIconResource ?: iconResource
    }
}

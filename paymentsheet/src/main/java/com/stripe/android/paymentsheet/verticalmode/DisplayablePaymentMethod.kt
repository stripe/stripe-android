package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.ui.IconHelper

internal data class DisplayablePaymentMethod(
    val code: PaymentMethodCode,
    val syntheticCode: String = code,
    val displayName: ResolvableString,
    @DrawableRes private val iconResource: Int,
    @DrawableRes val iconResourceNight: Int? = null,
    val lightThemeIconUrl: String?,
    val darkThemeIconUrl: String?,
    val iconRequiresTinting: Boolean,
    val subtitle: ResolvableString? = null,
    val promoBadge: String? = null,
    val onClick: () -> Unit,
    @DrawableRes private val outlinedIconResource: Int? = null,
) {
    @Composable
    @DrawableRes
    fun icon() = IconHelper.icon(iconResource, iconResourceNight, outlinedIconResource)

    @Composable
    fun iconUrl() = IconHelper.iconUrl(lightThemeIconUrl, darkThemeIconUrl)
}

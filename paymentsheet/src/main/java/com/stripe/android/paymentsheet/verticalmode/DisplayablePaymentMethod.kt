package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.DrawableRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCode

internal data class DisplayablePaymentMethod(
    val code: PaymentMethodCode,
    val displayName: ResolvableString,
    @DrawableRes val iconResource: Int,
    val lightThemeIconUrl: String?,
    val darkThemeIconUrl: String?,
    val iconRequiresTinting: Boolean,
    val subtitle: ResolvableString? = null,
    val promoBadge: String? = null,
    val onClick: () -> Unit,
)

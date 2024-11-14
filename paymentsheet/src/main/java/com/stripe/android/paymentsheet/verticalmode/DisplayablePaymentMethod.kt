package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.DrawableRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive

internal data class DisplayablePaymentMethod(
    val code: PaymentMethodCode,
    val displayName: ResolvableString,
    @DrawableRes val iconResource: Int,
    val lightThemeIconUrl: String?,
    val darkThemeIconUrl: String?,
    val iconRequiresTinting: Boolean,
    val subtitle: ResolvableString? = null,
    val incentive: PaymentMethodIncentive?,
    val onClick: () -> Unit,
)

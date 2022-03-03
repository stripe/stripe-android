package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentsheet.databinding.StripeGooglePayDividerBinding
import com.stripe.android.ui.core.PaymentSheetThemeConfig
import com.stripe.android.ui.core.isSystemDarkTheme

internal class GooglePayDivider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    @VisibleForTesting
    internal val viewBinding: StripeGooglePayDividerBinding = StripeGooglePayDividerBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    init {
        viewBinding.dividerText.setBackgroundColor(
            PaymentSheetThemeConfig.colors(context.isSystemDarkTheme()).surface.toArgb()
        )
        viewBinding.dividerText.setTextColor(
            PaymentSheetThemeConfig.colors(context.isSystemDarkTheme()).textSecondary.toArgb()
        )
    }

    fun setText(@StringRes resId: Int) {
        viewBinding.dividerText.setText(resId)
    }
}

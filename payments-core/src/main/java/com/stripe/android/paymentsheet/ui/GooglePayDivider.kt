package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import com.stripe.android.databinding.StripeGooglePayDividerBinding

internal class GooglePayDivider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private val viewBinding: StripeGooglePayDividerBinding = StripeGooglePayDividerBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    fun setText(@StringRes resId: Int) {
        viewBinding.dividerText.setText(resId)
    }
}

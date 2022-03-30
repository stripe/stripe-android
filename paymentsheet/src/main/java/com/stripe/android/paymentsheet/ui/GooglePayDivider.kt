package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeGooglePayDividerBinding
import com.stripe.android.ui.core.PaymentsTheme

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

    internal var text = ""

    fun setText(@StringRes resId: Int) {
        text = context.resources.getString(resId)
        viewBinding.dividerText.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GooglePayDividerUi(text)
            }
        }
    }

    @Composable
    private fun GooglePayDividerUi(
        text: String = stringResource(R.string.stripe_paymentsheet_or_pay_with_card)
    ) {
        Box(
            Modifier
                .background(PaymentsTheme.colors.material.surface)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                style = PaymentsTheme.typography.body1,
                color = PaymentsTheme.colors.subtitle
            )
        }
    }
}

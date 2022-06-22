package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding

internal class GooglePayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    internal val viewBinding = StripeGooglePayButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private var state: PrimaryButton.State? = null

    init {
        // Call super so we don't inadvertently effect the primary button as well.
        super.setClickable(true)
        super.setEnabled(true)
    }

    fun setBackgroundColor(useDarkResources: Boolean) {
        val buttonResources = ButtonResources.getResources(useDarkResources)

        viewBinding.googlePayButtonIcon.background = ResourcesCompat.getDrawable(
            resources,
            buttonResources.background,
            null
        )

        // Background resources for light button don't include a padding, while dark ones do.
        // Need to manually set the margins so they look the same size.
        val layoutParams =
            viewBinding.googlePayButtonIcon.layoutParams as RelativeLayout.LayoutParams
        layoutParams.setMargins(
            resources.getDimensionPixelSize(buttonResources.marginHorizontal),
            resources.getDimensionPixelSize(buttonResources.marginVertical),
            resources.getDimensionPixelSize(buttonResources.marginHorizontal),
            resources.getDimensionPixelSize(buttonResources.marginVertical)
        )

        viewBinding.googlePayButtonContent.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                buttonResources.content,
                null
            )
        )

        viewBinding.primaryButton.backgroundTintList =
            ColorStateList.valueOf(buttonResources.primaryButtonColor)
    }

    private fun onReadyState() {
        viewBinding.primaryButton.isVisible = false
        viewBinding.googlePayButtonIcon.isVisible = true
    }

    private fun onStartProcessing() {
        viewBinding.primaryButton.isVisible = true
        viewBinding.googlePayButtonIcon.isVisible = false
    }

    private fun onFinishProcessing() {
        viewBinding.primaryButton.isVisible = true
        viewBinding.googlePayButtonIcon.isVisible = false
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.primaryButton.isEnabled = enabled
        updateAlpha()
    }

    private fun updateAlpha() {
        viewBinding.googlePayButtonIcon.alpha =
            if ((state == null || state is PrimaryButton.State.Ready) && !isEnabled) {
                0.5f
            } else {
                1.0f
            }
    }

    fun updateState(state: PrimaryButton.State?) {
        viewBinding.primaryButton.updateState(state)
        this.state = state
        updateAlpha()

        when (state) {
            is PrimaryButton.State.Ready -> {
                onReadyState()
            }
            PrimaryButton.State.StartProcessing -> {
                onStartProcessing()
            }
            is PrimaryButton.State.FinishProcessing -> {
                onFinishProcessing()
            }
        }
    }

    private enum class ButtonResources(
        @DrawableRes val background: Int,
        @DrawableRes val content: Int,
        @ColorInt val primaryButtonColor: Int,
        @DimenRes val marginVertical: Int,
        @DimenRes val marginHorizontal: Int
    ) {
        Light(
            R.drawable.stripe_googlepay_button_no_shadow_background_light,
            R.drawable.stripe_googlepay_button_content_light,
            Color.WHITE,
            R.dimen.stripe_paymentsheet_googlepay_button_light_margin_vertical,
            R.dimen.stripe_paymentsheet_googlepay_button_light_margin_horizontal
        ),
        Dark(
            R.drawable.stripe_googlepay_button_no_shadow_background_dark,
            R.drawable.stripe_googlepay_button_content_dark,
            Color.BLACK,
            R.dimen.stripe_paymentsheet_googlepay_button_dark_margin,
            R.dimen.stripe_paymentsheet_googlepay_button_dark_margin
        );

        companion object {
            fun getResources(dark: Boolean) = if (dark) Dark else Light
        }
    }
}

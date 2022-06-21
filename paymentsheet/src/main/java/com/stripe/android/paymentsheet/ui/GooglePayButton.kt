package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
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
        val backgroundDrawable = if (useDarkResources) {
            R.drawable.stripe_googlepay_button_no_shadow_background_dark
        } else {
            R.drawable.stripe_googlepay_button_no_shadow_background_light
        }
        viewBinding.googlePayButtonIcon.background = ResourcesCompat.getDrawable(
            resources,
            backgroundDrawable,
            null
        )

        val contentDrawable = if (useDarkResources) R.drawable.stripe_googlepay_button_content_dark
        else R.drawable.stripe_googlepay_button_content_light

        viewBinding.googlePayButtonContent.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                contentDrawable,
                null
            )
        )

        val primaryButtonColor = if (useDarkResources) Color.BLACK else Color.WHITE
        viewBinding.primaryButton.backgroundTintList = (ColorStateList.valueOf(primaryButtonColor))
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
}

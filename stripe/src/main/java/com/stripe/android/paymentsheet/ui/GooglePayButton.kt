package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.stripe.android.databinding.StripeGooglePayButtonBinding

internal class GooglePayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    internal val viewBinding = StripeGooglePayButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    init {
        viewBinding.buyButton.background.setTint(Color.BLACK)
        isClickable = true
        isEnabled = false
    }

    // This is only needed for testing so we can click the button to iterate through
    // the states
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        viewBinding.buyButton.setOnClickListener(l)
    }

    private fun onReadyState() {
        viewBinding.buyButton.isVisible = false
        viewBinding.googlePayButtonIcon.isVisible = true
    }

    private fun onStartProcessing() {
        viewBinding.buyButton.isVisible = true
        viewBinding.googlePayButtonIcon.isVisible = false
    }

    private fun onFinishProcessing() {
        viewBinding.buyButton.isVisible = true
        viewBinding.googlePayButtonIcon.isVisible = false
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.buyButton.isEnabled = enabled
        viewBinding.buyButton.updateAlpha()
    }

    fun updateState(state: PrimaryButton.State?) {
        viewBinding.buyButton.updateAlpha()
        viewBinding.buyButton.updateState(state)

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

package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.stripe.android.R
import com.stripe.android.databinding.StripeGooglePayButtonBinding

internal class GooglePayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var state: State? = null
    private val animator = PrimaryButtonAnimator(context)

    internal val viewBinding = StripeGooglePayButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val confirmedIcon = viewBinding.confirmedIcon

    private var backgroundResource =
        R.drawable.stripe_googlepay_button_no_shadow_background

    init {
        super.setBackgroundResource(backgroundResource)

        viewBinding.label.text = getTextAttributeValue(attrs)

        isClickable = true
        isEnabled = false
    }

    override fun setBackgroundResource(resid: Int) {
        super.setBackgroundResource(resid)
        backgroundResource = resid
    }

    private fun getTextAttributeValue(attrs: AttributeSet?): CharSequence? {
        var text: CharSequence? = null
        context.withStyledAttributes(
            attrs,
            listOf(android.R.attr.text).toIntArray()
        ) {
            text = getText(0)
        }
        return text
    }

    private fun onReadyState(text: String?) {
        text?.let {
            viewBinding.label.text = text
        }
        viewBinding.confirmingIcon.isVisible = false
    }

    private fun onStartProcessing() {
        viewBinding.confirmingIcon.isVisible = true
        viewBinding.customerSupplied.isVisible = false

        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_primary_button_processing
        )
        viewBinding.label.isVisible = true
    }

    private fun onFinishProcessing(onAnimationEnd: () -> Unit) {

        super.setBackgroundResource(
            R.drawable.stripe_paymentsheet_primary_button_confirmed_background
        )

        animator.fadeOut(viewBinding.label)
        animator.fadeOut(viewBinding.confirmingIcon)

        animator.fadeIn(confirmedIcon, width) {
            super.setBackgroundResource(
                backgroundResource
            )
            viewBinding.customerSupplied.isVisible = true
            onAnimationEnd()
        }
        viewBinding.confirmingIcon.isVisible = false
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateAlpha()
    }

    fun updateState(state: State?) {
        this.state = state
        updateAlpha()

        when (state) {
            is State.Ready -> {
                onReadyState(state.label)
            }
            State.StartProcessing -> {
                onStartProcessing()
            }
            is State.FinishProcessing -> {
                onFinishProcessing(state.onComplete)
            }
        }
    }

    private fun updateAlpha() {
        viewBinding.label.alpha = if ((state == null || state is State.Ready) && !isEnabled) {
            0.5f
        } else {
            1.0f
        }
    }

    internal sealed class State {
        /**
         * The label will be applied if the value is not null.
         */
        data class Ready(val label: String? = null) : State()
        object StartProcessing : State()
        data class FinishProcessing(val onComplete: () -> Unit) : State()
    }
}

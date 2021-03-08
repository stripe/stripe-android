package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.stripe.android.R
import com.stripe.android.databinding.PrimaryButtonBinding

/**
 * The primary call-to-action for a payment sheet screen.
 */
internal class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var state: State? = null
    private val animator = PrimaryButtonAnimator(context)

    internal val viewBinding = PrimaryButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val confirmedIcon = viewBinding.confirmedIcon

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_primary_button_default_background)

        viewBinding.label.text = getTextAttributeValue(attrs)

        isClickable = true
        isEnabled = false
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
        viewBinding.lockIcon.isVisible = false
        viewBinding.confirmingIcon.isVisible = true

        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_primary_button_processing
        )
    }

    private fun onFinishProcessing(onAnimationEnd: () -> Unit) {
        setBackgroundResource(
            R.drawable.stripe_paymentsheet_primary_button_confirmed_background
        )

        animator.fadeOut(viewBinding.label)
        animator.fadeOut(viewBinding.confirmingIcon)

        animator.fadeIn(confirmedIcon, width) {
            onAnimationEnd()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.lockIcon.isVisible = enabled
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

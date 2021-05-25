package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
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
    private var defaultTintList: ColorStateList? = null
    private var state: State? = null
    private val animator = PrimaryButtonAnimator(context)

    // This is the text set by the client.  The internal label text is set to this value
    // in the on ready state and it is temporarily replaced during the processing and finishing states.
    private var externalLabel: String? = null

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

    override fun setBackgroundTintList(tintList: ColorStateList?) {
        super.setBackgroundTintList(tintList)
        defaultTintList = tintList
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

    fun setLabel(text: String?) {
        externalLabel = text
        externalLabel?.let {
            viewBinding.label.text = text
        }
    }

    private fun onReadyState() {
        externalLabel?.let {
            viewBinding.label.text = it
        }
        defaultTintList?.let {
            backgroundTintList = it
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
        super.setBackgroundTintList(null)
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
                onReadyState()
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
        object Ready : State()
        object StartProcessing : State()
        data class FinishProcessing(val onComplete: () -> Unit) : State()
    }
}

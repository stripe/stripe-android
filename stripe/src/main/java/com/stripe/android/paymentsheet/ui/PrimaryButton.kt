package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.stripe.android.R
import com.stripe.android.databinding.PrimaryButtonBinding
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

        isClickable = true
        isEnabled = false
    }

    fun setLabelText(text: String) {
        viewBinding.label.text = text
    }

    private fun onReadyState() {
        viewBinding.confirmingIcon.isVisible = false
    }

    private fun onConfirmState() {
        viewBinding.lockIcon.isVisible = false
        viewBinding.confirmingIcon.isVisible = true

        viewBinding.label.text = resources.getString(
            R.string.stripe_paymentsheet_pay_button_processing
        )
    }

    private suspend fun onCompletedStateBlocking(): Any =
        suspendCoroutine { cont ->
            onCompletedState {
                cont.resume(Any())
            }
        }

    private fun onCompletedState(onAnimationEnd: () -> Unit) {
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

    suspend fun updateState(state: State?) {
        this.state = state
        updateAlpha()

        when (state) {
            is State.Ready -> {
                setLabelText(state.label)
                onReadyState()
            }
            State.Confirming -> {
                onConfirmState()
            }
            is State.Completed -> {
                onCompletedStateBlocking()
            }
        }
    }

    private fun updateAlpha() {
        if ((state == null || state is State.Ready) && !isEnabled) {
            viewBinding.label.alpha = 0.5f
        } else {
            viewBinding.label.alpha = 1.0f
        }
    }

    internal sealed class State {
        data class Ready(val label: String) : State()
        object Confirming : State()
        object Completed : State()
    }
}

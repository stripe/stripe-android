package com.stripe.android.paymentsheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetAddButtonBinding
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator

/**
 * "Add" button for [PaymentOptionsActivity].
 */
internal class AddButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PrimaryButton(context, attrs, defStyleAttr) {
    private val animator = PrimaryButtonAnimator(context)

    private val _completedAnimation = MutableLiveData<AddButtonViewState.Completed>()
    internal val completedAnimation = _completedAnimation.distinctUntilChanged()

    private var viewState: AddButtonViewState? = null

    internal val viewBinding = PaymentSheetAddButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val confirmedIcon = viewBinding.confirmedIcon

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)

        isClickable = true
        isEnabled = false
        isGone = true
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.lockIcon.isVisible = enabled

        viewBinding.label.alpha = if (isEnabled) {
            1.0f
        } else {
            0.5f
        }
    }

    fun onReadyState() {
        viewBinding.confirmingIcon.isVisible = false
    }

    fun onConfirmingState() {
        viewBinding.lockIcon.isVisible = false
        viewBinding.confirmingIcon.isVisible = true

        viewBinding.label.text = resources.getString(
                R.string.stripe_paymentsheet_pay_button_processing
        )
    }

    private fun onCompletedState(state: AddButtonViewState.Completed) {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_confirmed_background)

        animator.fadeOut(viewBinding.label)
        animator.fadeOut(viewBinding.confirmingIcon)

        animateConfirmedIcon(state)
    }

    private fun animateConfirmedIcon(state: AddButtonViewState.Completed) {
        animator.fadeIn(confirmedIcon, width) {
            _completedAnimation.value = state
        }
    }


    fun updateState(viewState: AddButtonViewState) {
        this.viewState = viewState
        updateAlpha()

        when (viewState) {
            is AddButtonViewState.Ready -> {
                onReadyState()
            }
            AddButtonViewState.Confirming -> {
                onConfirmingState()
            }
            is AddButtonViewState.Completed -> {
                onCompletedState(viewState)
            }
        }
    }

    private fun updateAlpha() {
        if ((viewState == null || viewState is AddButtonViewState.Ready) && !isEnabled) {
            viewBinding.label.alpha = 0.5f
        } else {
            viewBinding.label.alpha = 1.0f
        }
    }
}

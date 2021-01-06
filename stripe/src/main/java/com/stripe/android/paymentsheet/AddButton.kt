package com.stripe.android.paymentsheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.R
import com.stripe.android.databinding.PaymentSheetAddButtonBinding
import com.stripe.android.paymentsheet.model.PaymentOptionViewState
import com.stripe.android.paymentsheet.ui.PrimaryButton

/**
 * "Add" button for [PaymentOptionsActivity].
 */
internal class AddButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PrimaryButton<PaymentOptionViewState>(context, attrs, defStyleAttr) {
    internal val viewBinding = PaymentSheetAddButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val _completed = MutableLiveData<PaymentOptionViewState.Completed>()
    internal val completed = _completed.distinctUntilChanged()

    init {
        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_default_background)

        isClickable = true
        isEnabled = false
    }

    fun onCompletedState(state: PaymentOptionViewState.Completed) {
        viewBinding.lockIcon.isVisible = false

        setBackgroundResource(R.drawable.stripe_paymentsheet_buy_button_confirmed_background)

        _completed.value = state
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.lockIcon.isVisible = enabled
    }

    override fun updateState(viewState: PaymentOptionViewState) {
        if (viewState is PaymentOptionViewState.Completed) {
            onCompletedState(viewState)
        }
    }
}

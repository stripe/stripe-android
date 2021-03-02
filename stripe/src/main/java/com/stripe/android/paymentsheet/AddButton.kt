package com.stripe.android.paymentsheet

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.R
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import java.util.Currency
import java.util.Locale

/**
 * Buy button for PaymentSheet.
 */
internal class AddButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DefaultPrimaryButton(context, attrs, defStyleAttr) {

    private val _completedAnimation = MutableLiveData<ViewState.Completed<*>>()
    internal val completedAnimation = _completedAnimation.distinctUntilChanged()

    private var viewState: ViewState? = null

    override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        setLockVisible(enabled)
        updateAlpha()
    }

    private fun updateAlpha() {
        if ((viewState == null || viewState is ViewState.Ready) && !isEnabled) {
            setLabelAlphaLow()
        } else {
            setLabelAlphaHigh()
        }
    }

    private fun getLabelText(state: ViewState.Ready) : String {
        return resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount,
            state.append
        )
    }

    fun updateState(viewState: ViewState) {
        this.viewState = viewState
        updateAlpha()

        when (viewState) {
            is ViewState.Ready -> {
                setReady(getLabelText(viewState))
            }
            ViewState.Confirming -> {
                setConfirm()
            }
            is ViewState.Completed<*> -> {
                setCompleted {
                    _completedAnimation.value = viewState
                }
            }
        }
    }

}

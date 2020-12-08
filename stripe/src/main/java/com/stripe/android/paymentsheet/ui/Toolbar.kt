package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.databinding.StripePaymentSheetToolbarBinding

internal class Toolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val mutableAction = MutableLiveData<Action>()
    internal val action: LiveData<Action> = mutableAction

    private val viewBinding = StripePaymentSheetToolbarBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    internal val closeButton = viewBinding.close
    internal val backButton = viewBinding.back

    init {
        closeButton.setOnClickListener { mutableAction.value = Action.Close }
        backButton.setOnClickListener { mutableAction.value = Action.Back }
    }

    fun showClose() {
        closeButton.isVisible = true
        backButton.isVisible = false
    }

    fun showBack() {
        closeButton.isVisible = false
        backButton.isVisible = true
    }

    fun updateProcessing(isProcessing: Boolean) {
        closeButton.isEnabled = !isProcessing
        backButton.isEnabled = !isProcessing
    }

    enum class Action {
        Close,
        Back
    }
}

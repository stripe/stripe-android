package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.databinding.StripePaymentSheetToolbarBinding

internal class Toolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val mutableAction = MutableLiveData<Action>()
    internal val action = mutableAction.distinctUntilChanged()

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
        closeButton.visibility = View.VISIBLE
        backButton.visibility = View.GONE
    }

    fun showBack() {
        closeButton.visibility = View.GONE
        backButton.visibility = View.VISIBLE
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

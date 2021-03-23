package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.appbar.MaterialToolbar
import com.stripe.android.R

internal class Toolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.toolbarStyle
) : MaterialToolbar(context, attrs, defStyleAttr) {

    private val _action = MutableLiveData<Action>()
    internal val action: LiveData<Action> = _action

    private lateinit var navigationAction: Action

    init {
        setNavigationOnClickListener {
            if (isEnabled) {
                _action.value = navigationAction
            }
        }
        showClose()
    }

    fun showClose() {
        navigationIcon =
            ContextCompat.getDrawable(context, R.drawable.stripe_paymentsheet_toolbar_close)
        navigationContentDescription = resources.getString(R.string.stripe_paymentsheet_close)
        navigationAction = Action.Close
    }

    fun showBack() {
        navigationIcon =
            ContextCompat.getDrawable(context, R.drawable.stripe_paymentsheet_toolbar_back)
        navigationContentDescription = resources.getString(R.string.stripe_paymentsheet_back)
        navigationAction = Action.Back
    }

    fun updateProcessing(isProcessing: Boolean) {
        isEnabled = !isProcessing
    }

    enum class Action {
        Close,
        Back
    }
}

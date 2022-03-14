package com.stripe.android.identity.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.LoadingButtonBinding

/**
 * A button with a progress indicator.
 */
internal class LoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(
    context, attrs, defStyleAttr
) {
    init {
        LoadingButtonBinding.inflate(LayoutInflater.from(context), this)
    }

    internal val button: MaterialButton = findViewById(R.id.button)

    /**
     * Disable button and show loading indicator.
     */
    fun toggleToLoading() {
        findViewById<MaterialButton>(R.id.button).isEnabled = false
        findViewById<CircularProgressIndicator>(R.id.indicator).visibility = View.VISIBLE
    }

    /**
     * Enable button and hide loading indicator.
     */
    fun toggleToButton() {
        findViewById<MaterialButton>(R.id.button).isEnabled = true
        findViewById<CircularProgressIndicator>(R.id.indicator).visibility = View.GONE

    }
}
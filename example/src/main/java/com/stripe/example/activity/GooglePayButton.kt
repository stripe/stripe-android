package com.stripe.example.activity

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.stripe.example.databinding.GooglePayButtonBinding

class GooglePayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        GooglePayButtonBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }
}

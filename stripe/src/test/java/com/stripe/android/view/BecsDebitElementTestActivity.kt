package com.stripe.android.view

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.R

internal class BecsDebitElementTestActivity : AppCompatActivity() {

    val becsDebitElement: BecsDebitElement by lazy {
        BecsDebitElement(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.StripeDefaultTheme)
        setContentView(FrameLayout(this).also {
            it.addView(becsDebitElement)
        })
    }
}

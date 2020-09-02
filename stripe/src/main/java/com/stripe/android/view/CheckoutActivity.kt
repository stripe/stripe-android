package com.stripe.android.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.databinding.ActivityCheckoutBinding

internal class CheckoutActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCheckoutBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }
}

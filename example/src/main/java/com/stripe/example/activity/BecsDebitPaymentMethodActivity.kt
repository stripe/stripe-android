package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.example.databinding.BecsDebitActivityBinding

class BecsDebitPaymentMethodActivity : AppCompatActivity() {

    private val viewBinding: BecsDebitActivityBinding by lazy {
        BecsDebitActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }
}

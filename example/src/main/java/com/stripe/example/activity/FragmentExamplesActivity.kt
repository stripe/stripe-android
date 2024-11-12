package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.example.R
import com.stripe.example.databinding.FragmentsExampleActivityBinding

class FragmentExamplesActivity : AppCompatActivity() {
    private val viewBinding: FragmentsExampleActivityBinding by lazy {
        FragmentsExampleActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setTitle(R.string.launch_payment_session_from_fragment)
    }
}

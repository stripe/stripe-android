package com.stripe.android.stripecardscan.example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.stripe.android.stripecardscan.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.buttonCardImageVerification.setOnClickListener {
            startActivity(Intent(this, CardImageVerificationDemoActivity::class.java))
        }

        viewBinding.buttonCardScan.setOnClickListener {
            startActivity(Intent(this, CardScanDemoActivity::class.java))
        }

        viewBinding.buttonCardScanFragment.setOnClickListener {
            startActivity(Intent(this, CardScanFragmentDemoActivity::class.java))
        }
    }
}

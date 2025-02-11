package com.stripe.android.stripecardscan.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.stripecardscan.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.buttonCardScan.setOnClickListener {
            startActivity(Intent(this, CardScanDemoActivity::class.java))
        }

        viewBinding.buttonCardScanFragment.setOnClickListener {
            startActivity(Intent(this, CardScanFragmentDemoActivity::class.java))
        }
    }
}

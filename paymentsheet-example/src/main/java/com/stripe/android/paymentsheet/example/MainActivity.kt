package com.stripe.android.paymentsheet.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.example.samples.activity.LaunchPaymentSheetCompleteActivity
import com.stripe.android.paymentsheet.example.samples.activity.LaunchPaymentSheetCustomActivity
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        viewBinding.launchCompleteButton.setOnClickListener {
            startActivity(Intent(this, LaunchPaymentSheetCompleteActivity::class.java))
        }

        viewBinding.launchCustomButton.setOnClickListener {
            startActivity(Intent(this, LaunchPaymentSheetCustomActivity::class.java))
        }

        viewBinding.launchPlaygroundButton.setOnClickListener {
            startActivity(Intent(this, PaymentSheetPlaygroundActivity::class.java))
        }
    }
}

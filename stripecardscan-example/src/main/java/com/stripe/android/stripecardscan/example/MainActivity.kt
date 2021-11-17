package com.stripe.android.stripecardscan.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.stripecardscan.example.activity.LaunchCardImageVerificationSheetCompleteActivity
import com.stripe.android.stripecardscan.example.activity.PARAM_PUBLISHABLE_KEY
import com.stripe.android.stripecardscan.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        viewBinding.launchCompleteButton.setOnClickListener {
            startActivity(
                Intent(this, LaunchCardImageVerificationSheetCompleteActivity::class.java)
                    .putExtra(
                        PARAM_PUBLISHABLE_KEY,
                        viewBinding.stripePublishableKey.text.toString(),
                    )
            )
        }
    }
}

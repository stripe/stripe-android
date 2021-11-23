package com.stripe.android.stripecardscan.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.stripe.android.stripecardscan.example.activity.LaunchCardImageVerificationSheetCompleteActivity
import com.stripe.android.stripecardscan.example.activity.PARAM_PUBLISHABLE_KEY
import com.stripe.android.stripecardscan.example.activity.PARAM_VERIFICATION_RESULT
import com.stripe.android.stripecardscan.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val requestQueue = Volley.newRequestQueue(this)

        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        val civLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            viewBinding.scanResultText.text = result.data?.getStringExtra(PARAM_VERIFICATION_RESULT)
        }

        viewBinding.getPublishableKeyButton.setOnClickListener {
            requestQueue.add(
                JsonObjectRequest(
                    "https://stripe-card-scan-civ-example-app.glitch.me/publishable_key",
                    { response ->
                        viewBinding.stripePublishableKey.setText(
                            response.get("publishable_key") as String
                        )
                        viewBinding.launchCompleteButton.isEnabled = true
                    },
                    { error ->
                        Toast.makeText(
                            this,
                            "Error getting publishable key: ${error.message}",
                            Toast.LENGTH_SHORT,
                        )
                            .show()
                    }
                )
            )
        }

        viewBinding.stripePublishableKey.addTextChangedListener(
            { _, _, _, _ -> /* before change */ },
            { _, _, _, _ -> /* on change */ },
            { s ->
                viewBinding.launchCompleteButton.isEnabled = !s.isNullOrEmpty()
            }
        )

        viewBinding.launchCompleteButton.setOnClickListener {
            civLauncher.launch(
                Intent(this, LaunchCardImageVerificationSheetCompleteActivity::class.java)
                    .putExtra(
                        PARAM_PUBLISHABLE_KEY,
                        viewBinding.stripePublishableKey.text.toString(),
                    )
            )
        }
    }
}

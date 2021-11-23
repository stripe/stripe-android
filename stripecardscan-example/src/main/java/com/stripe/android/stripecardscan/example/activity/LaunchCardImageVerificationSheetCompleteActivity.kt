package com.stripe.android.stripecardscan.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheet
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheetResult
import com.stripe.android.stripecardscan.example.databinding.ActivityLaunchCardImageVerificationSheetCompleteBinding
import org.json.JSONObject

internal const val PARAM_PUBLISHABLE_KEY = "stripePublishableKey"
internal const val PARAM_VERIFICATION_RESULT = "verificationResult"

internal class LaunchCardImageVerificationSheetCompleteActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityLaunchCardImageVerificationSheetCompleteBinding.inflate(layoutInflater)
    }

    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        requestQueue = Volley.newRequestQueue(this)

        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val stripePublishableKey = intent.getStringExtra(PARAM_PUBLISHABLE_KEY) ?: run {
            Log.e("Stripe Example", "Unable to read publishable key")
            finish()
            return@onCreate
        }

        val cardImageVerificationSheet =
            CardImageVerificationSheet.create(this, stripePublishableKey)

        viewBinding.generateCivIntent.setOnClickListener {
            requestQueue.add(
                JsonObjectRequest(
                    Request.Method.POST,
                    "https://stripe-card-scan-civ-example-app.glitch.me/card-set/checkout",
                    JSONObject().apply {
                        put("expected_card[iin]", viewBinding.requiredIin.text.toString())
                        put("expected_card[last4]", viewBinding.requiredLast4.text.toString())
                    },
                    { response ->
                        viewBinding.civIdText.setText(response.get("id") as String)
                        viewBinding.civSecretText.setText(response.get("client_secret") as String)
                        viewBinding.launchScanButton.isEnabled = true
                    },
                    { error ->
                        Toast.makeText(
                            this,
                            "Error generating CIV: ${error.message}",
                            Toast.LENGTH_SHORT,
                        )
                            .show()
                    },
                )
            )
        }

        viewBinding.civIdText.addTextChangedListener(
            { _, _, _, _ -> /* before change */ },
            { _, _, _, _ -> /* on change */ },
            { s ->
                viewBinding.launchScanButton.isEnabled =
                    !s.isNullOrEmpty() && !viewBinding.civSecretText.text.isNullOrEmpty()
            }
        )

        viewBinding.civSecretText.addTextChangedListener(
            { _, _, _, _ -> /* before change */ },
            { _, _, _, _ -> /* on change */ },
            { s ->
                viewBinding.launchScanButton.isEnabled =
                    !s.isNullOrEmpty() && !viewBinding.civIdText.text.isNullOrEmpty()
            }
        )

        viewBinding.launchScanButton.setOnClickListener {
            cardImageVerificationSheet.present(
                viewBinding.civIdText.text.toString(),
                viewBinding.civSecretText.text.toString(),
                this::onScanFinished,
            )
        }
    }

    private fun onScanFinished(result: CardImageVerificationSheetResult) {
        Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show()

        requestQueue.add(
            JsonObjectRequest(
                Request.Method.POST,
                "https://stripe-card-scan-civ-example-app.glitch.me/verify",
                JSONObject().apply {
                    put("civ_id", viewBinding.civIdText.text.toString())
                },
                { response ->
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(PARAM_VERIFICATION_RESULT, response.toString()),
                    )
                    finish()
                },
                { error ->
                    Toast.makeText(
                        this,
                        "Error getting validation payload: ${error.message}",
                        Toast.LENGTH_SHORT,
                    )
                        .show()
                },
            )
        )
    }
}

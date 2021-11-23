package com.stripe.android.stripecardscan.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheet
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheetResult
import com.stripe.android.stripecardscan.example.databinding.ActivityLaunchCardImageVerificationSheetCompleteBinding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val PARAM_PUBLISHABLE_KEY = "stripePublishableKey"
internal const val PARAM_VERIFICATION_RESULT = "verificationResult"

internal class LaunchCardImageVerificationSheetCompleteActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityLaunchCardImageVerificationSheetCompleteBinding.inflate(layoutInflater)
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
            Fuel.post("https://stripe-card-scan-civ-example-app.glitch.me/card-set/checkout")
                .header("content-type", "application/json")
                .body(
                    """
                    {
                        "expected_card[iin]": "${viewBinding.requiredIinText.text}",
                        "expected_card[last4]": "${viewBinding.requiredLast4Text.text}"
                    }
                    """.trimIndent()
                )
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> snackbarController
                            .show("Error generating CIV: ${result.getException().message}")
                        is Result.Success -> runOnUiThread {
                            json.decodeFromString(CivCreationResponse.serializer(), result.get())
                                .let {
                                    viewBinding.civIdText.setText(it.civId)
                                    viewBinding.civSecretText.setText(it.civClientSecret)
                                    viewBinding.launchScanButton.isEnabled = true
                                }
                        }
                    }
                }
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
        snackbarController.show(result.toString())

        Fuel.post("https://stripe-card-scan-civ-example-app.glitch.me/verify")
            .header("content-type", "application/json")
            .body(
                """
                {
                    "civ_id": "${viewBinding.civIdText.text}"
                }
                """.trimIndent()
            )
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> snackbarController
                        .show("Error getting validation payload: ${result.getException().message}")
                    is Result.Success -> {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(PARAM_VERIFICATION_RESULT, result.get()),
                        )
                        finish()
                    }
                }
            }
    }

    @Serializable
    data class CivCreationResponse(
        @SerialName("publishable_key") val publishableKey: String,
        @SerialName("id") val civId: String,
        @SerialName("client_secret") val civClientSecret: String,
    )
}

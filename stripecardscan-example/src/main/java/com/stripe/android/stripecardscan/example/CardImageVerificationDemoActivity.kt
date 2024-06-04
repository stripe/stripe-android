package com.stripe.android.stripecardscan.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheet
import com.stripe.android.stripecardscan.cardimageverification.CardImageVerificationSheetResult
import com.stripe.android.stripecardscan.example.databinding.ActivityCardImageVerificationDemoBinding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CardImageVerificationDemoActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCardImageVerificationDemoBinding.inflate(layoutInflater)
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

    private val settings by lazy { Settings(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val cardImageVerificationSheet =
            CardImageVerificationSheet.create(
                this,
                settings.publishableKey,
                config = CardImageVerificationSheet.Configuration(
                    strictModeFrames = CardImageVerificationSheet.Configuration.StrictModeFrameCount.Low
                ),
                cardImageVerificationResultCallback = this::onScanFinished
            )

        viewBinding.generateCivIntent.setOnClickListener {
            Fuel.post("${settings.backendUrl}/card-set/checkout")
                .header("content-type", "application/json")
                .body(
                    json.encodeToString(
                        CivCreationRequest.serializer(),
                        CivCreationRequest(
                            iin = viewBinding.requiredIinText.text.toString(),
                            lastFour = viewBinding.requiredLast4Text.text.toString(),
                        ),
                    )
                )
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure ->
                            snackbarController
                                .show("Error generating CIV: ${result.getException().message}")
                        is Result.Success -> runOnUiThread {
                            try {
                                json.decodeFromString(
                                    CivCreationResponse.serializer(),
                                    result.get()
                                )
                                    .let {
                                        viewBinding.scanResultText.setText("")
                                        viewBinding.civIdText.setText(it.civId)
                                        viewBinding.civSecretText.setText(it.civClientSecret)
                                        viewBinding.launchScanButton.isEnabled = true
                                    }
                            } catch (t: Throwable) {
                                viewBinding.scanResultText.text = result.value
                                viewBinding.civIdText.setText("")
                                viewBinding.civSecretText.setText("")
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
                viewBinding.civSecretText.text.toString()
            )
        }
    }

    private fun onScanFinished(result: CardImageVerificationSheetResult) {
        snackbarController.show(result.toString())

        Fuel.post("${settings.backendUrl}/verify")
            .header("content-type", "application/json")
            .body(
                json.encodeToString(
                    VerifyRequest.serializer(),
                    VerifyRequest(
                        civId = viewBinding.civIdText.text.toString()
                    )
                )
            )
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure ->
                        snackbarController
                            .show("Error getting validation payload: ${result.getException().message}")
                    is Result.Success -> runOnUiThread {
                        viewBinding.scanResultText.text = result.value
                        viewBinding.civIdText.setText("")
                        viewBinding.civSecretText.setText("")
                    }
                }
            }
    }

    @Serializable
    data class CivCreationRequest(
        @SerialName("expected_card[iin]") val iin: String,
        @SerialName("expected_card[last4]") val lastFour: String,
    )

    @Serializable
    data class CivCreationResponse(
        @SerialName("id") val civId: String,
        @SerialName("client_secret") val civClientSecret: String,
    )

    @Serializable
    data class VerifyRequest(
        @SerialName("civ_id") val civId: String,
    )
}

package com.stripe.android.stripecardscan.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.stripe.android.stripecardscan.example.activity.LaunchCardImageVerificationSheetCompleteActivity
import com.stripe.android.stripecardscan.example.activity.PARAM_PUBLISHABLE_KEY
import com.stripe.android.stripecardscan.example.activity.PARAM_VERIFICATION_RESULT
import com.stripe.android.stripecardscan.example.activity.SnackbarController
import com.stripe.android.stripecardscan.example.databinding.ActivityMainBinding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
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
        setSupportActionBar(findViewById(R.id.toolbar))

        val civLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            viewBinding.scanResultText.text = result.data?.getStringExtra(PARAM_VERIFICATION_RESULT)
        }

        viewBinding.getPublishableKeyButton.setOnClickListener {
            Fuel.get("https://stripe-card-scan-civ-example-app.glitch.me/publishable_key")
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> runOnUiThread {
                            snackbarController
                                .show("Error getting key: ${result.getException().message}")
                        }
                        is Result.Success -> runOnUiThread {
                            json.decodeFromString(PubKeyResponse.serializer(), result.get())
                                .let {
                                    viewBinding.stripePublishableKey.setText(it.publishableKey)
                                    viewBinding.launchCompleteButton.isEnabled = true
                                }
                        }
                    }
                }
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

    @Serializable
    data class PubKeyResponse(
        @SerialName("publishable_key") val publishableKey: String,
    )
}

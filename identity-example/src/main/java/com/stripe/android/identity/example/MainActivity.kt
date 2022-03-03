package com.stripe.android.identity.example

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.databinding.ActivityMainBinding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    lateinit var identityVerificationSheet: IdentityVerificationSheet

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        identityVerificationSheet =
            IdentityVerificationSheet.create(
                this,
                IdentityVerificationSheet.Configuration(
                    merchantLogo = R.drawable.merchant_logo,
                    stripePublishableKey = "pk_test"
                )
            )

        binding.startVerification.setOnClickListener {
            binding.startVerification.isEnabled = false
            binding.progressCircular.visibility = View.VISIBLE
            showSnackBar(
                "Getting verificationSessionId and ephemeralKeySecret from backend...",
                Snackbar.LENGTH_LONG
            )


            Fuel.post(EXAMPLE_BACKEND_URL)
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            showSnackBar("Error generating verificationSessionId and ephemeralKeySecret: ${result.getException().message}")
                            binding.progressCircular.visibility = View.INVISIBLE
                            binding.startVerification.isEnabled = true
                        }
                        is Result.Success -> runOnUiThread {
                            binding.progressCircular.visibility = View.INVISIBLE
                            binding.startVerification.isEnabled = true
                            try {
                                json.decodeFromString(
                                    VerificationSessionCreationResponse.serializer(),
                                    result.get()
                                ).let {
                                    identityVerificationSheet.present(
                                        verificationSessionId = it.verificationSessionId,
                                        ephemeralKeySecret = it.ephemeralKeySecret
                                    ) {
                                        Snackbar.make(
                                            binding.root,
                                            "Verification result: $it",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (t: Throwable) {
                                showSnackBar("Fail to decode")
                            }
                        }
                    }
                }
        }
    }

    private fun showSnackBar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(binding.root, message, duration)
            .show()
    }

    @Serializable
    data class VerificationSessionCreationResponse(
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("ephemeral_key_secret") val ephemeralKeySecret: String,
        @SerialName("id") val verificationSessionId: String,
    )

    private companion object {
        const val EXAMPLE_BACKEND_URL =
            "https://reflective-fossil-rib.glitch.me/create-verification-session"
    }
}

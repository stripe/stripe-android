package com.stripe.android.identity.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    lateinit var identityVerificationSheet: IdentityVerificationSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        identityVerificationSheet =
            IdentityVerificationSheet.create(
                this,
                IdentityVerificationSheet.Configuration(
                    merchantLogo = R.drawable.stripe_logo,
                    stripePublishableKey = "pk_test"
                )
            )

        binding.startVerification.setOnClickListener {
            identityVerificationSheet.present(
                verificationSessionId = "testVerificationSessionId",
                ephemeralKeySecret = "testEphemeralKeySecret"
            ) {
                Snackbar.make(
                    binding.root, "Verification result: $it", Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
}

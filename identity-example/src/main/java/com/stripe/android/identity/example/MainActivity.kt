package com.stripe.android.identity.example

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
                Log.d(TAG, "verification result: $it")
            }
        }
    }

    private companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }
}

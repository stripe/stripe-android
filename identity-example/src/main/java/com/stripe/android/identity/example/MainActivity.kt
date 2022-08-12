package com.stripe.android.identity.example

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.databinding.ActivityMainBinding

abstract class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var identityVerificationSheet: IdentityVerificationSheet

    protected abstract val getBrandLogoResId: Int

    private val viewModel: IdentityExampleViewModel by viewModels()

    private val logoUri: Uri
        get() = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(getBrandLogoResId))
            .appendPath(resources.getResourceTypeName(getBrandLogoResId))
            .appendPath(resources.getResourceEntryName(getBrandLogoResId))
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        identityVerificationSheet =
            IdentityVerificationSheet.create(
                this,
                IdentityVerificationSheet.Configuration(
                    // Or use webImage by
                    // brandLogo = Uri.parse("https://path/to/a/logo.jpg")
                    brandLogo = logoUri
                )
            ) {
                when (it) {
                    IdentityVerificationSheet.VerificationFlowResult.Canceled -> {
                        binding.resultView.text = "Verification result: ${it.javaClass.simpleName}"
                    }
                    IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                        binding.resultView.text = "Verification result: ${it.javaClass.simpleName}"
                    }
                    is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                        binding.resultView.text =
                            "Verification result: ${it.javaClass.simpleName} - ${it.throwable}"
                    }
                }
            }

        binding.useNative.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.requireIdNumber.isChecked = false
                binding.requireMatchingSelfie.isChecked = false
                binding.requireIdNumber.isEnabled = false
            }
        }

        binding.useWeb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.requireIdNumber.isChecked = false
                binding.requireMatchingSelfie.isChecked = false
                binding.requireIdNumber.isEnabled = true
            }
        }

        binding.startVerification.setOnClickListener {
            binding.startVerification.isEnabled = false
            binding.resultView.text = ""
            binding.vsView.text = ""
            binding.progressCircular.visibility = View.VISIBLE
            showSnackBar(
                "Getting verificationSessionId and ephemeralKeySecret from backend...",
                Snackbar.LENGTH_LONG
            )

            viewModel.postForResult(
                allowDrivingLicense = binding.allowedTypeDl.isChecked,
                allowPassport = binding.allowedTypePassport.isChecked,
                allowId = binding.allowedTypeId.isChecked,
                requireLiveCapture = binding.requireLiveCapture.isChecked,
                requireId = binding.requireIdNumber.isChecked,
                requireSelfie = binding.requireMatchingSelfie.isChecked
            ).observe(this) { result ->
                binding.progressCircular.visibility = View.INVISIBLE
                binding.startVerification.isEnabled = true
                when (result) {
                    is Result.Failure -> {
                        binding.resultView.text =
                            "Error generating verificationSessionId and ephemeralKeySecret: ${result.getException().message}"
                    }
                    is Result.Success -> runOnUiThread {
                        binding.vsView.text = result.value.verificationSessionId
                        if (binding.useNative.isChecked) {
                            identityVerificationSheet.present(
                                verificationSessionId = result.value.verificationSessionId,
                                ephemeralKeySecret = result.value.ephemeralKeySecret
                            )
                        } else {
                            CustomTabsIntent.Builder().build()
                                .launchUrl(this, Uri.parse(result.value.url))
                            binding.resultView.text = "web redirect"
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
}

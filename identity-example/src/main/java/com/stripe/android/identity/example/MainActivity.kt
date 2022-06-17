package com.stripe.android.identity.example

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.databinding.ActivityMainBinding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

abstract class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var identityVerificationSheet: IdentityVerificationSheet

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
    }

    protected abstract val getBrandLogoResId: Int

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

            Fuel.post(EXAMPLE_BACKEND_URL)
                .header("content-type", "application/json")
                .body(
                    json.encodeToString(
                        VerificationSessionCreationRequest.serializer(),
                        VerificationSessionCreationRequest(
                            options = VerificationSessionCreationRequest.Options(
                                document = VerificationSessionCreationRequest.Document(
                                    requireIdNumber = binding.requireIdNumber.isChecked,
                                    requireMatchingSelfie = binding.requireMatchingSelfie.isChecked,
                                    requireLiveCapture = binding.requireLiveCapture.isChecked,
                                    allowedTypes = mutableListOf<String>().also {
                                        if (binding.allowedTypeDl.isChecked) it.add(DRIVING_LICENSE)
                                        if (binding.allowedTypePassport.isChecked) it.add(PASSPORT)
                                        if (binding.allowedTypeId.isChecked) it.add(ID_CARD)
                                    }
                                )
                            )
                        )
                    )
                )
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            binding.resultView.text =
                                "Error generating verificationSessionId and ephemeralKeySecret: ${result.getException().message}"
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
                                    binding.vsView.text = it.verificationSessionId
                                    if (binding.useNative.isChecked) {
                                        identityVerificationSheet.present(
                                            verificationSessionId = it.verificationSessionId,
                                            ephemeralKeySecret = it.ephemeralKeySecret
                                        )
                                    } else {
                                        CustomTabsIntent.Builder().build()
                                            .launchUrl(this, Uri.parse(it.url))
                                    }
                                }
                            } catch (t: Throwable) {
                                binding.resultView.text = "Fail to decode"
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
        @SerialName("url") val url: String,
    )

    @Serializable
    data class VerificationSessionCreationRequest(
        @SerialName("options") val options: Options? = null,
        @SerialName("type") val type: String = "document",
    ) {
        @Serializable
        data class Options(
            @SerialName("document") val document: Document? = null
        )

        @Serializable
        data class Document(
            @SerialName("allowed_types") val allowedTypes: List<String>? = null,
            @SerialName("require_id_number") val requireIdNumber: Boolean? = null,
            @SerialName("require_live_capture") val requireLiveCapture: Boolean? = null,
            @SerialName("require_matching_selfie") val requireMatchingSelfie: Boolean? = null
        )
    }

    private companion object {
        const val DRIVING_LICENSE = "driving_license"
        const val PASSPORT = "passport"
        const val ID_CARD = "id_card"
        const val EXAMPLE_BACKEND_URL =
            "https://reflective-fossil-rib.glitch.me/create-verification-session"
    }
}

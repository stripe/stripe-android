package com.stripe.android.hcaptcha.example

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.stripe.android.Stripe
import com.stripe.android.createRadarSession
import com.stripe.hcaptcha.HCaptcha
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaTokenResponse
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnOpenListener
import com.stripe.hcaptcha.task.OnSuccessListener
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.hcaptcha.example.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

class MainActivity : FragmentActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var hCaptcha: HCaptcha? = null
    private var tokenResponse: HCaptchaTokenResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(HCaptchaSize.NORMAL, HCaptchaSize.INVISIBLE, HCaptchaSize.COMPACT)
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        viewBinding.sizes.adapter = adapter

        // Toggle verbose webview logs
        viewBinding.webViewDebug.setOnCheckedChangeListener { _, isChecked ->
            WebView.setWebContentsDebuggingEnabled(isChecked)
        }

        viewBinding.reset.setOnClickListener {
            hCaptcha?.reset()
            setTokenTextView("-")
            hCaptcha = null
            viewBinding.verify.isEnabled = false
        }

        viewBinding.setup.setOnClickListener {
            hCaptcha = HCaptcha.getClient(this).setup(getConfig())
            setupClient(hCaptcha)
            viewBinding.verify.isEnabled = true
        }

        viewBinding.verify.setOnClickListener {
            setTokenTextView("-")
            hCaptcha?.verifyWithHCaptcha() ?: {
                hCaptcha = HCaptcha.getClient(this).setup(getConfig()).verifyWithHCaptcha()
                setupClient(hCaptcha)
            }
        }

        viewBinding.markUsed.setOnClickListener {
            tokenResponse?.markUsed()
        }

        viewBinding.hitTest.setOnClickListener {
            Toast.makeText(this, "Hit Test!", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "onHitTest")
        }
    }

    private fun getSizeFromSpinner() = viewBinding.sizes.selectedItem as HCaptchaSize

    private fun getConfig() = HCaptchaConfig(
        siteKey = SITEKEY,
        size = getSizeFromSpinner(),
        loading = viewBinding.loading.isChecked,
        hideDialog = viewBinding.hideDialog.isChecked,
        tokenExpiration = 10.seconds,
        retryPredicate = { _, exception -> exception.hCaptchaError === HCaptchaError.SESSION_TIMEOUT }
    )

    private fun setTokenTextView(text: String) {
        viewBinding.tokenTextView.text = text
        viewBinding.errorTextView.text = "-"
    }

    private fun setErrorTextView(error: String) {
        viewBinding.tokenTextView.text = "-"
        viewBinding.errorTextView.text = error
    }

    private fun setupClient(hCaptcha: HCaptcha?) {
        hCaptcha
            ?.addOnSuccessListener(object : OnSuccessListener<HCaptchaTokenResponse> {
                override fun onSuccess(result: HCaptchaTokenResponse) {
                    tokenResponse = result
                    val userResponseToken: String = result.tokenResult
                    setTokenTextView(userResponseToken)
                }
            })
            ?.addOnFailureListener(object : OnFailureListener {
                override fun onFailure(exception: HCaptchaException) {
                    Log.d(
                        Companion.TAG,
                        "hCaptcha failed: " + exception.message + "(" + exception.statusCode + ")"
                    )
                    setErrorTextView(exception.message)
                    tokenResponse = null
                }
            })
            ?.addOnOpenListener(object : OnOpenListener {
                override fun onOpen() {
                    Toast.makeText(
                        this@MainActivity,
                        "hCaptcha shown",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    companion object {
        private const val TAG: String = "hCaptcha example"
        private const val SITEKEY = "10000000-ffff-ffff-ffff-000000000001"
    }
}
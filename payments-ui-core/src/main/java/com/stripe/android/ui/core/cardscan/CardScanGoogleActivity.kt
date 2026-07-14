package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ui.core.R

internal class CardScanGoogleActivity : AppCompatActivity() {
    private val paymentCardRecognitionClient: PaymentCardRecognitionClient by lazy {
        paymentCardRecognitionClientFactory()
    }
    private var hasLaunchedGoogleCardScanner = false

    private val cardScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        setResult(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasLaunchedGoogleCardScanner =
            savedInstanceState?.getBoolean(HAS_LAUNCHED_GOOGLE_CARD_SCANNER) == true
        setContentView(R.layout.stripe_activity_card_scan)

        if (!hasLaunchedGoogleCardScanner) {
            fetchAndLaunchGoogleCardScanner()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HAS_LAUNCHED_GOOGLE_CARD_SCANNER, hasLaunchedGoogleCardScanner)
        super.onSaveInstanceState(outState)
    }

    private fun fetchAndLaunchGoogleCardScanner() {
        paymentCardRecognitionClient.fetchIntent(
            context = this,
            onFailure = ::finishWithFailure,
            onSuccess = ::launchGoogleCardScanner,
        )
    }

    private fun launchGoogleCardScanner(intentSenderRequest: IntentSenderRequest) {
        if (isFinishing || isDestroyed) {
            return
        }

        hasLaunchedGoogleCardScanner = true
        runCatching {
            cardScanLauncher.launch(intentSenderRequest)
        }.onFailure(::finishWithFailure)
    }

    private fun finishWithFailure(error: Throwable) {
        if (isFinishing || isDestroyed) {
            return
        }

        setResult(
            RESULT_CARD_SCAN_FAILED,
            createFailureIntent(error.message)
        )
        finish()
    }

    companion object {
        internal const val RESULT_CARD_SCAN_FAILED = Activity.RESULT_FIRST_USER

        private const val HAS_LAUNCHED_GOOGLE_CARD_SCANNER = "has_launched_google_card_scanner"
        private const val EXTRA_ERROR_MESSAGE = "extra_error_message"

        @VisibleForTesting
        internal var paymentCardRecognitionClientFactory: () -> PaymentCardRecognitionClient = {
            DefaultPaymentCardRecognitionClient()
        }

        internal fun createIntent(context: Context): Intent {
            return Intent(context, CardScanGoogleActivity::class.java)
        }

        internal fun createFailureIntent(errorMessage: String?): Intent {
            return Intent().putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }

        internal fun getErrorMessage(intent: Intent?): String? {
            return intent?.getStringExtra(EXTRA_ERROR_MESSAGE)
        }
    }
}

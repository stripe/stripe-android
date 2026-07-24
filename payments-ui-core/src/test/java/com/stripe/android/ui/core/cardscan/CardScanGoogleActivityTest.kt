package com.stripe.android.ui.core.cardscan

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.activity.result.IntentSenderRequest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class CardScanGoogleActivityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        CardScanGoogleActivity.paymentCardRecognitionClientFactory = {
            DefaultPaymentCardRecognitionClient()
        }
    }

    @Test
    fun `activity returns failed result when fetching Google intent fails`() {
        CardScanGoogleActivity.paymentCardRecognitionClientFactory = {
            FakePaymentCardRecognitionClient(shouldSucceed = false)
        }

        ActivityScenario.launchActivityForResult<CardScanGoogleActivity>(
            CardScanGoogleActivity.createIntent(context)
        ).use { scenario ->
            waitForIdle()

            assertThat(scenario.result.resultCode).isEqualTo(CardScanGoogleActivity.RESULT_CARD_SCAN_FAILED)
            assertThat(CardScanGoogleActivity.getErrorMessage(scenario.result.resultData))
                .isEqualTo("Failed to fetch intent")
        }
    }

    @Test
    fun `activity ignores Google intent result after it has finished`() {
        val paymentCardRecognitionClient = ControllablePaymentCardRecognitionClient()
        CardScanGoogleActivity.paymentCardRecognitionClientFactory = {
            paymentCardRecognitionClient
        }

        ActivityScenario.launchActivityForResult<CardScanGoogleActivity>(
            CardScanGoogleActivity.createIntent(context)
        ).use { scenario ->
            scenario.onActivity { activity ->
                activity.finish()
            }
            waitForIdle()

            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)

            paymentCardRecognitionClient.completeFetch(context)
            waitForIdle()

            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        }
    }

    private fun waitForIdle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private class ControllablePaymentCardRecognitionClient : PaymentCardRecognitionClient {
        private var pendingSuccess: ((IntentSenderRequest) -> Unit)? = null

        override fun fetchIntent(
            context: Context,
            onFailure: (Throwable) -> Unit,
            onSuccess: (IntentSenderRequest) -> Unit
        ) {
            pendingSuccess = onSuccess
        }

        fun completeFetch(context: Context) {
            val onSuccess = checkNotNull(pendingSuccess)
            pendingSuccess = null
            onSuccess(createIntentSenderRequest(context))
        }
    }
}

private fun createIntentSenderRequest(context: Context): IntentSenderRequest {
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(),
        PendingIntent.FLAG_IMMUTABLE
    )

    return IntentSenderRequest.Builder(pendingIntent.intentSender).build()
}

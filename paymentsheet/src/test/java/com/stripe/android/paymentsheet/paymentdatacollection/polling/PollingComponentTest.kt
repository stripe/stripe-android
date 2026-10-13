package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Application
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.polling.di.DaggerPollingComponent
import com.stripe.android.polling.IntentStatusPoller
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
internal class PollingComponentTest {
    @get:Rule
    val networkRule = NetworkRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    // Keep the ViewModel's clock paused while the test waits for real network I/O.
    // Otherwise runTest can advance its timeout and trigger a second request.
    @Test
    fun `Polling uses request options bound in the component`() = runTest(
        UnconfinedTestDispatcher(TestCoroutineScheduler())
    ) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val component = DaggerPollingComponent.factory().create(
            application = application,
            config = IntentStatusPoller.Config(CLIENT_SECRET, 1.seconds),
            ioDispatcher = Dispatchers.IO,
            requestOptions = REQUEST_OPTIONS,
        )
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_polling"),
            header("Authorization", "Bearer pk_test_polling_component"),
            header("Stripe-Account", "acct_polling_component"),
            applyDefaultAuthorization = false,
        ) { response ->
            response.setBody(
                """{"id":"pi_polling","object":"payment_intent","status":"succeeded"}"""
            )
        }

        val viewModelStore = ViewModelStore()
        val viewModel = component.subcomponentFactory.create(
            savedStateHandle = SavedStateHandle(),
            args = PollingViewModel.Args(
                clientSecret = CLIENT_SECRET,
                timeLimit = 60.seconds,
                initialDelay = 0.seconds,
                pollingInterval = 1.seconds,
                ctaText = R.string.stripe_blik_confirm_payment,
                requestOptions = REQUEST_OPTIONS,
                qrCodeUrl = null,
                paymentMethodType = "blik",
            ),
        ).viewModel
        viewModelStore.put("polling", viewModel)
        try {
            viewModel.uiState.test {
                var state = awaitItem()
                while (state.pollingState == PollingState.Active) {
                    state = awaitItem()
                }
                assertThat(state.pollingState).isEqualTo(PollingState.Success)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            viewModelStore.clear()
        }
    }

    @Test
    fun `Polling activity arguments preserve request options through parceling`() {
        val args = PollingContract.Args(
            clientSecret = CLIENT_SECRET,
            statusBarColor = null,
            timeLimitInSeconds = 60,
            initialDelayInSeconds = 0,
            pollingIntervalInSeconds = 1,
            ctaText = R.string.stripe_blik_confirm_payment,
            requestOptions = REQUEST_OPTIONS,
            qrCodeUrl = null,
            paymentMethodType = "blik",
        )
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(args, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            val restored = requireNotNull(parcel.readParcelable<PollingContract.Args>(javaClass.classLoader))
            assertThat(restored.requestOptions).isEqualTo(REQUEST_OPTIONS)
            assertThat(restored.clientSecret).isEqualTo(args.clientSecret)
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        const val CLIENT_SECRET = "pi_polling_secret_example"
        val REQUEST_OPTIONS = ApiRequest.Options("pk_test_polling_component", "acct_polling_component")
    }
}

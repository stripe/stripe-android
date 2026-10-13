package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.polling.di.DaggerPollingComponent
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
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

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    // Keep the ViewModel's clock paused while the test waits for real network I/O.
    // Otherwise runTest can advance its timeout and trigger a second request.
    @Test
    fun `Polling uses configuration bound in the component`() = runTest(
        UnconfinedTestDispatcher(TestCoroutineScheduler())
    ) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val component = DaggerPollingComponent.factory().create(
            application = application,
            config = IntentStatusPoller.Config(CLIENT_SECRET, 1.seconds),
            ioDispatcher = Dispatchers.IO,
            apiConfiguration = API_CONFIGURATION,
        )
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_polling"),
        ) { response ->
            response.setBody(
                """{"id":"pi_polling","object":"payment_intent","status":"succeeded"}"""
            )
        }

        val viewModel = component.subcomponentFactory.create(
            savedStateHandle = SavedStateHandle(),
            args = PollingViewModel.Args(
                clientSecret = CLIENT_SECRET,
                timeLimit = 60.seconds,
                initialDelay = 0.seconds,
                pollingInterval = 1.seconds,
                ctaText = R.string.stripe_blik_confirm_payment,
                apiConfiguration = API_CONFIGURATION,
                qrCodeUrl = null,
                paymentMethodType = "blik",
            ),
        ).viewModel.also { viewModelStoreRule.track(it) }
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.pollingState == PollingState.Active) {
                state = awaitItem()
            }
            assertThat(state.pollingState).isEqualTo(PollingState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val CLIENT_SECRET = "pi_polling_secret_example"
        val API_CONFIGURATION = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_ACCOUNT_ID,
        )
    }
}

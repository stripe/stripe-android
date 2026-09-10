package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnrampCoordinatorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `retained coordinator restores callbacks after its host finishes`() = runTest {
        val results = Turbine<OnrampAuthorizeResult>()
        val callbacks = OnrampCallbacks()
            .verifyIdentityCallback { error("Unexpected identity result") }
            .collectPaymentCallback { error("Unexpected payment result") }
            .authorizeCallback { results.add(it) }
            .checkoutCallback { error("Unexpected checkout result") }
            .verifyKycCallback { error("Unexpected KYC result") }
            .onrampSessionClientSecretProvider { error("Unexpected checkout request") }
        val coordinator = OnrampCoordinator.Builder().build(
            ApplicationProvider.getApplicationContext(),
            SavedStateHandle(),
            callbacks,
        )
        val registered = OnrampCallbackReferences[DEFAULT_ONRAMP_INSTANCE_KEY]
        assertThat(registered).isNotNull()
        val first = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            coordinator.createPresenter(first.get())
            first.get().finish()
            first.pause().stop().destroy()
            assertThat(OnrampCallbackReferences[DEFAULT_ONRAMP_INSTANCE_KEY]).isNull()

            val second = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            try {
                val presenter = coordinator.createPresenter(second.get())
                assertThat(OnrampCallbackReferences[DEFAULT_ONRAMP_INSTANCE_KEY]).isSameInstanceAs(registered)
                // No backend configuration: the SDK must deliver its validation error
                // through the original callback rather than throw for missing callbacks.
                presenter.authorize("lai_test")
                assertThat(results.awaitItem()).isInstanceOf(OnrampAuthorizeResult.Failed::class.java)
                results.expectNoEvents()
            } finally {
                second.get().finish()
                second.pause().stop().destroy()
            }
        } finally {
            OnrampCallbackReferences.remove(DEFAULT_ONRAMP_INSTANCE_KEY)
            results.ensureAllEventsConsumed()
        }
    }
}

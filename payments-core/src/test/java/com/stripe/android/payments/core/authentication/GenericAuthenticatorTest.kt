package com.stripe.android.payments.core.authentication

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenericAuthenticatorTest {

    private val lifecycleOwner = TestLifecycleOwner()

    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn lifecycleOwner
    }

    @Test
    fun `Only starts authentication flow once lifecycle changes to resumed`() = runTest {
        val authenticator = FakeResumedNextActionHandler()
        lifecycleOwner.currentState = Lifecycle.State.CREATED

        val completable = CompletableDeferred<Unit>()

        launch(backgroundScope.coroutineContext) {
            authenticator.performNextAction(
                host = host,
                actionable = mock(),
                apiConfiguration = API_CONFIGURATION,
            )
            completable.complete(Unit)
        }

        testScheduler.runCurrent()
        authenticator.nextActionCalls.expectNoEvents()

        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        completable.await()

        assertThat(authenticator.nextActionCalls.awaitItem()).isSameInstanceAs(API_CONFIGURATION)
        authenticator.nextActionCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `Immediately starts authentication flow if lifecycle already resumed`() = runTest {
        val authenticator = FakeResumedNextActionHandler()
        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        authenticator.performNextAction(
            host = host,
            actionable = mock(),
            apiConfiguration = API_CONFIGURATION,
        )

        assertThat(authenticator.nextActionCalls.awaitItem()).isSameInstanceAs(API_CONFIGURATION)
        authenticator.nextActionCalls.ensureAllEventsConsumed()
    }

    private companion object {
        val API_CONFIGURATION = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
            betas = emptySet(),
        )
    }
}

internal class FakeResumedNextActionHandler : PaymentNextActionHandler<StripeIntent>() {

    val nextActionCalls = Turbine<ApiConfiguration.State>()

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        apiConfiguration: ApiConfiguration.State
    ) {
        nextActionCalls.add(apiConfiguration)
    }
}

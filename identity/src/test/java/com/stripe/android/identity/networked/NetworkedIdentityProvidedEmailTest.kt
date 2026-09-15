package com.stripe.android.identity.networked

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class NetworkedIdentityProvidedEmailTest {
    @Test
    fun `first appearance automatically signs in once with the validated candidate`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        viewModel.onFirstAppearance("consumer@example.com")
        viewModel.onFirstAppearance("different@example.com")
        runCurrent()
        assertThat(repository.lookupCalls.awaitItem().email).isEqualTo("consumer@example.com")
        repository.lookupCalls.expectNoEvents()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.LookupPending)
    }

    @Test
    fun `rejected candidate consumes automatic sign-in opportunity`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        viewModel.onFirstAppearance(null)
        viewModel.onFirstAppearance("consumer@example.com")
        runCurrent()
        repository.lookupCalls.expectNoEvents()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
    }

    @Test
    fun `automatic sign-in cannot restart after cancellation before appearance`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        viewModel.cancel()
        cancellations.awaitItem()
        viewModel.onFirstAppearance("consumer@example.com")
        runCurrent()
        repository.lookupCalls.expectNoEvents()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
    }

    @Test
    fun `session expiry after automatic sign-in requires explicit resubmission`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        viewModel.onFirstAppearance("consumer@example.com")
        runCurrent()
        repository.lookupCalls.awaitItem().response.complete(Result.success(niFound()))
        runCurrent()
        repository.startCalls.awaitItem().response.complete(Result.failure(niError("consumer_session_expired")))
        runCurrent()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.ReauthenticationRequired)

        viewModel.onFirstAppearance("consumer@example.com")
        runCurrent()
        repository.lookupCalls.expectNoEvents()
        viewModel.submitEmail("consumer@example.com")
        runCurrent()
        assertThat(repository.lookupCalls.awaitItem().email).isEqualTo("consumer@example.com")
    }

    @Test
    fun `first appearance checks current coordinator state`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        startEmail().response.complete(Result.failure(niError("consumer_session_expired")))
        runCurrent()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.ReauthenticationRequired)
        viewModel.onFirstAppearance("consumer@example.com")
        runCurrent()
        repository.lookupCalls.expectNoEvents()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.ReauthenticationRequired)
    }

    @Test
    fun `Activity recreation retains consumed opportunity`() = runNetworkedIdentityScenario {
        val creations = Turbine<Unit>()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                creations.add(Unit)
                return NetworkedIdentityViewModel(coordinator) as T
            }
        }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val original = ViewModelProvider(activity.get(), factory)[NetworkedIdentityViewModel::class.java]
        creations.awaitItem()
        original.onFirstAppearance(null)
        activity.recreate()
        val retained = ViewModelProvider(activity.get(), factory)[NetworkedIdentityViewModel::class.java]
        assertThat(retained).isSameInstanceAs(original)
        retained.onFirstAppearance("consumer@example.com")
        runCurrent()
        assertThat(retained.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
        repository.lookupCalls.expectNoEvents()
        creations.expectNoEvents()
        activity.pause().stop().destroy()
        runCurrent()
        creations.ensureAllEventsConsumed()
    }
}

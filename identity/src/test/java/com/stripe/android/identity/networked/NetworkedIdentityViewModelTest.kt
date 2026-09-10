package com.stripe.android.identity.networked

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class NetworkedIdentityViewModelTest {
    @Test
    fun `Activity recreation retains flow until permanent destruction`() = runNetworkedIdentityScenario {
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
        awaitOtp()

        activity.recreate()
        val retained = ViewModelProvider(activity.get(), factory)[NetworkedIdentityViewModel::class.java]
        assertThat(retained).isSameInstanceAs(original)
        assertThat(retained.state.value).isInstanceOf(NetworkedIdentityState.AwaitingOtp::class.java)
        creations.expectNoEvents()
        repository.logoutCalls.expectNoEvents()

        activity.pause().stop().destroy()
        runCurrent()
        assertThat(repository.logoutCalls.awaitItem().credentials.sessionClientSecret).isEqualTo("session_started")
        assertThat(retained.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        cancellations.expectNoEvents()
        fallbacks.expectNoEvents()
        creations.ensureAllEventsConsumed()
    }

    @Test
    fun `cleared ViewModel still cleans up a late lookup success`() = runNetworkedIdentityScenario {
        val store = ViewModelStore()
        val viewModel = NetworkedIdentityViewModel(coordinator)
        store.put("networked", viewModel)
        viewModel.submitEmail("person@example.com")
        runCurrent()
        val lookup = repository.lookupCalls.awaitItem()
        store.clear()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        lookup.response.complete(Result.success(niFound()))
        runCurrent()
        val logout = repository.logoutCalls.awaitItem()
        assertThat(logout.credentials.sessionClientSecret).isEqualTo("session_lookup")
        assertThat(logout.authSessionSecrets).containsExactly("auth_lookup")
        repository.startCalls.expectNoEvents()
        cancellations.expectNoEvents()
        fallbacks.expectNoEvents()
    }

    @Test
    fun `cleared ViewModel cleans current and late confirmation credentials`() = runNetworkedIdentityScenario {
        val store = ViewModelStore()
        store.put("networked", NetworkedIdentityViewModel(coordinator))
        awaitOtp()
        val confirm = confirmOtp()
        store.clear()
        runCurrent()
        assertThat(repository.logoutCalls.awaitItem().credentials.sessionClientSecret).isEqualTo("session_started")
        confirm.response.complete(
            Result.success(niResponse(clientSecret = "late_rotation", authSessionClientSecret = "late_auth"))
        )
        runCurrent()
        val logout = repository.logoutCalls.awaitItem()
        assertThat(logout.credentials.sessionClientSecret).isEqualTo("late_rotation")
        assertThat(logout.authSessionSecrets).containsExactly("auth_lookup", "auth_started", "late_auth").inOrder()
        cancellations.expectNoEvents()
        fallbacks.expectNoEvents()
    }

    @Test
    fun `explicit external dismissal abandons a retained owner`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        awaitOtp()
        viewModel.abandon()
        runCurrent()
        repository.logoutCalls.awaitItem()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        cancellations.expectNoEvents()
        fallbacks.expectNoEvents()
    }
}

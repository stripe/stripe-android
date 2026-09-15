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
    fun `Activity recreation retains the attempt until permanent destruction`() = runNetworkedIdentityScenario {
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
        original.startReuse()
        runCurrent()
        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()

        activity.recreate()
        val retained = ViewModelProvider(activity.get(), factory)[NetworkedIdentityViewModel::class.java]
        assertThat(retained).isSameInstanceAs(original)
        assertThat(retained.state.value).isEqualTo(NetworkedIdentityState.CollectEmail)
        creations.expectNoEvents()

        activity.pause().stop().destroy()
        runCurrent()
        assertThat(retained.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        outcomes.expectNoEvents()
        creations.ensureAllEventsConsumed()
    }

    @Test
    fun `cleared ViewModel ignores a late response`() = runNetworkedIdentityScenario {
        val store = ViewModelStore()
        val viewModel = NetworkedIdentityViewModel(coordinator)
        store.put("networked", viewModel)
        viewModel.startReuse()
        runCurrent()
        val configure = linkSession.configureCalls.awaitItem()

        store.clear()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        configure.response.complete(Result.success(Unit))
        runCurrent()

        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.Cancelled)
        outcomes.expectNoEvents()
    }

    @Test
    fun `screen actions drive the attempt`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        viewModel.startReuse()
        runCurrent()
        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()

        viewModel.screenActions().onSubmitEmail("person@example.com")
        runCurrent()

        assertThat(linkSession.lookupCalls.awaitItem().email).isEqualTo("person@example.com")
    }
}

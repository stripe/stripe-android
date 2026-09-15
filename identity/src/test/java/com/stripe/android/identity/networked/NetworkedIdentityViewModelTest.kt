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

        linkSession.linkUICalls.awaitItem()

        activity.recreate()
        val retained = ViewModelProvider(activity.get(), factory)[NetworkedIdentityViewModel::class.java]
        assertThat(retained).isSameInstanceAs(original)
        assertThat(retained.state.value).isEqualTo(NetworkedIdentityState.LinkAuthentication)
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
        reachDocuments(listOf(niDocument("document_1"), niDocument("document_2")))

        viewModel.screenActions().onSelectDocument("document_2")

        assertThat((viewModel.state.value as NetworkedIdentityState.SelectDocument).selectedDocumentId)
            .isEqualTo("document_2")
    }

    @Test
    fun `the sheet stays hidden while Link's own screens authenticate`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        viewModel.startReuse()
        runCurrent()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.Preparing)
        assertThat(viewModel.isSheetVisible.value).isFalse()

        linkSession.configureCalls.awaitItem().response.complete(Result.success(Unit))
        runCurrent()
        val linkUI = linkSession.linkUICalls.awaitItem()
        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.LinkAuthentication)
        assertThat(viewModel.isSheetVisible.value).isFalse()

        linkUI.response.complete(Result.success(niAccount(isVerified = true)))
        runCurrent()
        repository.documentCalls.awaitItem()
        assertThat(viewModel.isSheetVisible.value).isTrue()
    }

    @Test
    fun `manual capture from the intro records the skip without showing the sheet`() =
        runNetworkedIdentityScenario {
            val viewModel = NetworkedIdentityViewModel(coordinator)

            viewModel.chooseManualCaptureFromIntro()
            runCurrent()
            assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
            assertThat(viewModel.isSheetVisible.value).isFalse()
            actions.skipCalls.awaitItem().response.complete(Result.success(niActionPageData()))
            assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
            runCurrent()

            assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.ManualCapture)
        }

    @Test
    fun `manual capture from the sheet shows its progress`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator)
        reachDocuments()

        viewModel.screenActions().onManualCapture()
        runCurrent()

        assertThat(viewModel.state.value).isEqualTo(NetworkedIdentityState.SkipPending)
        assertThat(viewModel.isSheetVisible.value).isTrue()
        actions.skipCalls.awaitItem().response.complete(Result.success(niActionPageData()))
        assertThat(verificationUpdates.awaitItem()).isEqualTo(niActionPageData())
        runCurrent()
        assertThat(outcomes.awaitItem()).isEqualTo(NetworkedIdentityOutcome.ManualCapture)
    }
}

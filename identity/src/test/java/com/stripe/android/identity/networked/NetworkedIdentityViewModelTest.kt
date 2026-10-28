package com.stripe.android.identity.networked

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class NetworkedIdentityViewModelTest {
    @Test
    fun `cleared ViewModel ignores a late response`() = runNetworkedIdentityScenario {
        val store = ViewModelStore()
        val viewModel = NetworkedIdentityViewModel(coordinator, includesSelfie = false)
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
    fun `the sheet stays hidden while Link's own screens authenticate`() = runNetworkedIdentityScenario {
        val viewModel = NetworkedIdentityViewModel(coordinator, includesSelfie = false)
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
}

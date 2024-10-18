package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.NavHostController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val vm = LinkActivityViewModel(mock())
    private val navController: NavHostController = mock()
    private val dismissWithResult: (LinkActivityResult) -> Unit = mock()

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm.dismissWithResult = dismissWithResult
        vm.navController = navController
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that cancel result is called on back pressed with empty stack`() = runTest(dispatcher) {
        whenever(navController.popBackStack()).thenReturn(false)

        vm.handleViewAction(LinkAction.BackPressed)

        verify(navController).popBackStack()
        verify(dismissWithResult).invoke(LinkActivityResult.Canceled())
    }

    @Test
    fun `test that cancel result is called on back pressed with non-empty stack`() = runTest(dispatcher) {
        whenever(navController.popBackStack()).thenReturn(true)

        vm.handleViewAction(LinkAction.BackPressed)

        verify(navController).popBackStack()
        verify(dismissWithResult, times(0)).invoke(LinkActivityResult.Canceled())
    }

    @Test
    fun `test that activity unregister removes dismissWithResult and nav controller`() = runTest(dispatcher) {
        vm.unregisterActivity()

        assertThat(vm.dismissWithResult).isEqualTo(null)
        assertThat(vm.navController).isEqualTo(null)
    }

    @Test
    fun `initializer throws exception when args are null`() {
        val savedStateHandle = SavedStateHandle()
            .apply {
                set(LinkActivity.EXTRA_ARGS, null)
            }

        val factory = LinkActivityViewModel.factory(savedStateHandle)

        assertFailsWith<NoArgsException> {
            factory.create(LinkActivityViewModel::class.java, creationExtras())
        }.also { exception ->
            assertThat(exception.message).isEqualTo("NativeLinkArgs not found")
        }
    }

    @Test
    fun `initializer creates ViewModel when args are valid`() {
        val mockArgs = NativeLinkArgs(
            configuration = mock(),
            publishableKey = "",
            stripeAccountId = null
        )
        val savedStateHandle = SavedStateHandle()
        val factory = LinkActivityViewModel.factory(savedStateHandle)
        savedStateHandle[LinkActivity.EXTRA_ARGS] = mockArgs

        val viewModel = factory.create(LinkActivityViewModel::class.java, creationExtras())
        assertThat(viewModel.activityRetainedComponent.configuration).isEqualTo(mockArgs.configuration)
    }

    private fun creationExtras(): CreationExtras {
        val mockOwner = mock<SavedStateRegistryOwner>()
        val mockViewModelStoreOwner = mock<ViewModelStoreOwner>()
        whenever(mockViewModelStoreOwner.viewModelStore).thenReturn(ViewModelStore())
        return MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, mockOwner)
            set(APPLICATION_KEY, application)
            set(VIEW_MODEL_STORE_OWNER_KEY, mockViewModelStoreOwner)
            set(VIEW_MODEL_KEY, LinkActivityViewModel::class.java.name)
        }
    }
}

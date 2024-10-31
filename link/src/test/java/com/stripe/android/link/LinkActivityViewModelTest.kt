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
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.PopUpToBuilder
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.FakeLinkAccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val vm = LinkActivityViewModel(mock(), FakeLinkAccountManager())
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

    @Test
    fun `goBack should dismiss with cancel result when pop back stack fails`() = runTest(dispatcher) {
        whenever(navController.popBackStack()).thenReturn(false)

        vm.goBack()

        verify(navController).popBackStack()
        verify(dismissWithResult).invoke(LinkActivityResult.Canceled())
    }

    @Test
    fun `goBack should not dismiss when pop back stack succeeds`() = runTest(dispatcher) {
        whenever(navController.popBackStack()).thenReturn(true)

        vm.goBack()

        verify(navController).popBackStack()
        verify(dismissWithResult, times(0)).invoke(LinkActivityResult.Canceled())
    }

    @Test
    fun `navigate should change route clear back stack inclusively when clearStack is True`() = runTest(dispatcher) {
        val startDestinationId = 123
        val navGraph: NavGraph = mock()

        whenever(navController.graph).thenReturn(navGraph)
        whenever(navGraph.startDestinationId).thenReturn(startDestinationId)

        vm.navigate(LinkScreen.Wallet, clearStack = true)

        argumentCaptor<NavOptionsBuilder.() -> Unit>().apply {
            verify(navController).navigate(eq(LinkScreen.Wallet.route), capture())

            val navOptionsBuilder: NavOptionsBuilder = mock()
            firstValue.invoke(navOptionsBuilder)

            argumentCaptor<PopUpToBuilder.() -> Unit>().apply {
                verify(navOptionsBuilder).popUpTo(eq(startDestinationId), capture())

                val popUpToBuilder = PopUpToBuilder()
                firstValue.invoke(popUpToBuilder)

                assertThat(popUpToBuilder.inclusive).isTrue()
            }
        }
    }

    @Test
    fun `navigate should only change route when clearStack is False`() = runTest(dispatcher) {
        val startDestinationId = 123
        val navGraph: NavGraph = mock()

        whenever(navController.graph).thenReturn(navGraph)
        whenever(navGraph.startDestinationId).thenReturn(startDestinationId)

        vm.navigate(LinkScreen.Wallet, clearStack = false)

        argumentCaptor<NavOptionsBuilder.() -> Unit>().apply {
            verify(navController).navigate(eq(LinkScreen.Wallet.route), capture())

            val navOptionsBuilder: NavOptionsBuilder = mock()
            firstValue.invoke(navOptionsBuilder)

            verify(navOptionsBuilder, times(0)).popUpTo(eq(startDestinationId), any())
        }
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

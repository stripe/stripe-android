package com.stripe.android.link

import androidx.navigation.NavHostController
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

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val vm = LinkActivityViewModel()
    private val navController: NavHostController = mock()
    private val dismissWithResult: (LinkActivityResult) -> Unit = mock()

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
}

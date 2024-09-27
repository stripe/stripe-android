package com.stripe.android.link

import androidx.navigation.NavHostController
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
import org.mockito.kotlin.verify
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
    fun `test that cancel result is called on back pressed`() = runTest(dispatcher) {
        vm.handleViewAction(LinkAction.BackPressed)

        verify(dismissWithResult).invoke(LinkActivityResult.Canceled())
    }
}

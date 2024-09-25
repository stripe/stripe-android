package com.stripe.android.link

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val vm = LinkActivityViewModel()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that viewmodel has correct initial state`() = runTest(dispatcher) {
        vm.state.test {
            Truth.assertThat(awaitItem()).isEqualTo(LinkState)
        }
    }

    @Test
    fun `test that correct effect is emitted on back pressed`() = runTest(dispatcher) {
        vm.effect.test {
            vm.handleAction(LinkAction.BackPressed)
            Truth.assertThat(awaitItem()).isEqualTo(LinkEffect.GoBack)
        }
    }
}

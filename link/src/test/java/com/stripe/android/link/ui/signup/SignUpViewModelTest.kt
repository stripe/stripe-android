package com.stripe.android.link.ui.signup

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
class SignUpViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val vm = SignUpViewModel()

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
            Truth.assertThat(awaitItem()).isEqualTo(SignUpViewState())
        }
    }

    @Test
    fun `test that correct effect is emitted on successful sign up`() = runTest(dispatcher) {
        vm.effect.test {
            vm.handleAction(SignUpAction.SignUpClicked)
            Truth.assertThat(awaitItem()).isEqualTo(SignUpEffect.NavigateToWallet)
        }
    }
}

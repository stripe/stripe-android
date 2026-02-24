package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TapToAddResult
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class TapToAddNavigatorTest {
    @Test
    fun `screen emits initial screen when constructed with initial screen`() = runTest {
        val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor)
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            initialScreen = initialScreen,
        )

        navigator.screen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
        }
    }

    @Test
    fun `performAction with Close event emits Canceled with null payment selection on result flow`() = runTest {
        val initialScreen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor)
        val navigator = TapToAddNavigator(
            coroutineScope = this,
            initialScreen = initialScreen,
        )

        navigator.result.test {
            navigator.performAction(TapToAddNavigator.Action.Close)
            assertThat(awaitItem()).isEqualTo(TapToAddResult.Canceled(paymentSelection = null))
        }
    }

    private object FakeTapToAddCollectingInteractor : TapToAddCollectingInteractor
}

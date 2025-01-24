package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.Closeable
import kotlin.time.Duration.Companion.milliseconds

internal class NavigationHandlerTest {
    @Test
    fun `currentScreen is initialized to Loading`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `transitionTo removes Loading`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val newScreen = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `transitionTo works with shouldRemoveInitialScreenOnTransition=false`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = this,
            initialScreen = PaymentSheetScreen.Loading,
            shouldRemoveInitialScreenOnTransition = false
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val newScreen = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(navigationHandler.canGoBack).isTrue()
            navigationHandler.pop()
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
        }
    }

    @Test
    fun `transitionTo keeps backstack`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `transitionToWithDelay transitions to target screen`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = testScope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionToWithDelay(screenTwo)
            testScope.testScheduler.advanceTimeBy(250.milliseconds)
            ensureAllEventsConsumed()
            testScope.testScheduler.advanceTimeBy(1.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `transitionToWithDelay allows future transitions`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = testScope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionToWithDelay(screenTwo)
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            val screenThree = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenThree)
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `transitionToWithDelay prevents other navigation actions during delay`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = testScope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionToWithDelay(screenTwo)
            val screenThree = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenThree)
            navigationHandler.pop()
            navigationHandler.resetTo(listOf(screenThree))
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `resetTo resets backstack`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            val screenThree = mock<PaymentSheetScreen>()
            navigationHandler.resetTo(listOf(screenThree))
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `resetTo calls close on removed screens`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            val screenThree = mock<PaymentSheetScreen>()
            navigationHandler.resetTo(listOf(screenThree))
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isFalse()
            verify(screenOne as Closeable).close()
            verify(screenTwo as Closeable).close()
        }
    }

    @Test
    fun `resetTo only calls close on removed screens`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            val screenThree = mock<PaymentSheetScreen>()
            navigationHandler.resetTo(listOf(screenTwo, screenThree))
            assertThat(awaitItem()).isEqualTo(screenThree)
            verify(screenOne as Closeable).close()
            verify(screenTwo as Closeable, never()).close()
        }
    }

    @Test
    fun `pop removes the top screen from the backstack`() = runTest {
        var calledPopHandler = false
        val screenOne = mock<PaymentSheetScreen>()
        val screenTwo = mock<PaymentSheetScreen>()

        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {
            calledPopHandler = true
            assertThat(it).isEqualTo(screenTwo)
        }

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)

            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)

            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()

            navigationHandler.pop()
            assertThat(calledPopHandler).isTrue()
            assertThat(awaitItem()).isEqualTo(screenOne)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `pop removes the top screen from the backstack and calls close`() = runTest {
        var calledPopHandler = false

        val screenOne = mock<PaymentSheetScreen>()
        val screenTwo = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))

        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {
            calledPopHandler = true
            assertThat(it).isEqualTo(screenTwo)
        }

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)

            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)

            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()

            navigationHandler.pop()
            assertThat(calledPopHandler).isTrue()
            assertThat(awaitItem()).isEqualTo(screenOne)
            assertThat(navigationHandler.canGoBack).isFalse()
            verify(screenTwo as Closeable).close()
        }
    }

    @Test
    fun `popWithDelay transitions to target screen`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = testScope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            navigationHandler.popWithDelay()
            testScope.testScheduler.advanceTimeBy(250.milliseconds)
            ensureAllEventsConsumed()
            testScope.testScheduler.advanceTimeBy(1.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenOne)
        }
    }

    @Test
    fun `popWithDelay allows future transitions`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = testScope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            navigationHandler.popWithDelay()
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenThree = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenThree)
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `popWithDelay prevents other navigation actions during delay`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = testScope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            navigationHandler.popWithDelay()
            val screenThree = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenThree)
            navigationHandler.pop()
            navigationHandler.resetTo(listOf(screenThree))
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenOne)
        }
    }

    @Test
    fun `closeScreens calls close on all closable screens in the backstack`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            navigationHandler.closeScreens()
            verify(screenOne as Closeable).close()
            verify(screenTwo as Closeable).close()
        }
    }
}

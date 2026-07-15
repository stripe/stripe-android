package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun `cancelling the coroutine scope closes all closable screens in the backstack`() = runTest {
        val scope = CoroutineScope(Job())
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(scope, PaymentSheetScreen.Loading) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            scope.cancel()
            verify(screenOne as Closeable).close()
            verify(screenTwo as Closeable).close()
        }
    }

    @Test
    fun `transitionTo closes the initial screen when it is removed from the backstack`() = runTest {
        val initialScreen = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, initialScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            val newScreen = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            verify(initialScreen as Closeable).close()
        }
    }

    @Test
    fun `transitionTo does not close the initial screen when it is kept on the backstack`() = runTest {
        val initialScreen = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = this,
            initialScreen = initialScreen,
            shouldRemoveInitialScreenOnTransition = false,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            val newScreen = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            verify(initialScreen as Closeable, never()).close()
        }
    }

    @Test
    fun `cancelling the coroutine scope closes a pending delayed transition that has not been applied`() = runTest {
        val scope = CoroutineScope(Job() + UnconfinedTestDispatcher(testScheduler))
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = scope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val pendingScreen = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionToWithDelay(pendingScreen)
            // The transition delay has not elapsed, so the target is not on the back stack yet.
            scope.cancel()
            verify(pendingScreen as Closeable).close()
        }
    }

    @Test
    fun `transitionToWithDelay closes a target dropped while a transition is already in flight`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(
            coroutineScope = testScope,
            initialScreen = PaymentSheetScreen.Loading,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val firstTarget = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionToWithDelay(firstTarget)
            val droppedTarget = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionToWithDelay(droppedTarget)
            verify(droppedTarget as Closeable).close()
            verify(firstTarget as Closeable, never()).close()
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(firstTarget)
        }
    }

    @Test
    fun `cancelling the coroutine scope closes the screens on the backstack`() = runTest {
        val scope = CoroutineScope(Job())
        val initialScreen = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
        NavigationHandler<PaymentSheetScreen>(scope, initialScreen) {}

        scope.cancel()

        verify(initialScreen as Closeable).close()
    }

    @Test
    fun `previousScreen value is correct`() = runTest {
        val navigationHandler = NavigationHandler<PaymentSheetScreen>(this, PaymentSheetScreen.Loading) {}
        navigationHandler.previousScreen.test {
            // Initially, there is no previous screen.
            assertThat(awaitItem()).isNull()

            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            // The previous screen doesn't get updated here -- Loading is removed from the backstack as part of the
            // initial loading. The previous screen is still null.

            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenOne)
        }
    }
}

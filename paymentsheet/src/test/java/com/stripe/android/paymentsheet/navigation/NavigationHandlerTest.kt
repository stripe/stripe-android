package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CleanupTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.Closeable
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

internal class NavigationHandlerTest {
    @get:Rule
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @Test
    fun `currentScreen is initialized to Loading`() = runTest {
        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `currentScreen is initialized to last screen in initial back stack`() = runTest {
        val screenOne = FakeScreen()
        val screenTwo = FakeScreen()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = this,
            initialBackStack = listOf(screenOne, screenTwo),
            shouldRemoveInitialScreenOnTransition = true,
        ) {}

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `pop from initial back stack returns to previous screen and closes popped screen`() = runTest {
        val screenOne = FakeCloseableScreen()
        val screenTwo = FakeCloseableScreen()
        var poppedScreen: TestScreen? = null
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = this,
            initialBackStack = listOf(screenOne, screenTwo),
            shouldRemoveInitialScreenOnTransition = true,
        ) {
            poppedScreen = it
        }

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(screenTwo)

            navigationHandler.pop()

            assertThat(awaitItem()).isEqualTo(screenOne)
            assertThat(navigationHandler.canGoBack).isFalse()
            assertThat(poppedScreen).isEqualTo(screenTwo)
            assertThat(screenTwo.closeCalls.awaitItem()).isEqualTo(Unit)
            screenTwo.validate()
            screenOne.validate()
        }
    }

    @Test
    fun `initial back stack cannot be empty`() = runTest {
        val error = assertFailsWith<IllegalArgumentException> {
            NavigationHandler<TestScreen>(
                coroutineScope = this,
                initialBackStack = emptyList(),
                shouldRemoveInitialScreenOnTransition = true,
            ) {}
        }

        assertThat(error).hasMessageThat().isEqualTo("Initial back stack cannot be empty.")
    }

    @Test
    fun `transitionTo removes Loading`() = runTest {
        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val newScreen = FakeScreen()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `transitionTo works with shouldRemoveInitialScreenOnTransition=false`() = runTest {
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = this,
            initialScreen = LoadingScreen,
            shouldRemoveInitialScreenOnTransition = false
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val newScreen = FakeScreen()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(navigationHandler.canGoBack).isTrue()
            navigationHandler.pop()
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
        }
    }

    @Test
    fun `transitionTo keeps backstack`() = runTest {
        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `transitionToWithDelay transitions to target screen`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
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
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
            navigationHandler.transitionToWithDelay(screenTwo)
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            val screenThree = FakeScreen()
            navigationHandler.transitionTo(screenThree)
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `transitionToWithDelay prevents other navigation actions during delay`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
            navigationHandler.transitionToWithDelay(screenTwo)
            val screenThree = FakeScreen()
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
        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            val screenThree = FakeScreen()
            navigationHandler.resetTo(listOf(screenThree))
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `resetTo calls close on removed screens`() = runTest {
        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeCloseableScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeCloseableScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            val screenThree = FakeScreen()
            navigationHandler.resetTo(listOf(screenThree))
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isFalse()
            assertThat(screenOne.closeCalls.awaitItem()).isEqualTo(Unit)
            assertThat(screenTwo.closeCalls.awaitItem()).isEqualTo(Unit)
            screenOne.validate()
            screenTwo.validate()
        }
    }

    @Test
    fun `resetTo only calls close on removed screens`() = runTest {
        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeCloseableScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeCloseableScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            val screenThree = FakeScreen()
            navigationHandler.resetTo(listOf(screenTwo, screenThree))
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(screenOne.closeCalls.awaitItem()).isEqualTo(Unit)
            screenOne.validate()
            screenTwo.validate()
        }
    }

    @Test
    fun `replaceCurrentScreen preserves backstack and closes replaced screen`() = runTest {
        val initialScreen = FakeScreen()
        val replacedScreen = FakeCloseableScreen()
        val replacementScreen = FakeScreen()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = this,
            initialScreen = initialScreen,
            shouldRemoveInitialScreenOnTransition = false,
        ) {}

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            navigationHandler.transitionTo(replacedScreen)
            assertThat(awaitItem()).isEqualTo(replacedScreen)

            val replaced = navigationHandler.replaceCurrentScreen(replacementScreen)

            assertThat(replaced).isTrue()
            assertThat(awaitItem()).isEqualTo(replacementScreen)
            assertThat(navigationHandler.previousScreen.value).isEqualTo(initialScreen)
            assertThat(navigationHandler.canGoBack).isTrue()
            assertThat(replacedScreen.closeCalls.awaitItem()).isEqualTo(Unit)
            replacedScreen.validate()
        }
    }

    @Test
    fun `replaceCurrentScreen replaces root without adding to backstack`() = runTest {
        val initialScreen = FakeCloseableScreen()
        val replacementScreen = FakeScreen()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = this,
            initialScreen = initialScreen,
            shouldRemoveInitialScreenOnTransition = false,
        ) {}

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)

            val replaced = navigationHandler.replaceCurrentScreen(replacementScreen)

            assertThat(replaced).isTrue()
            assertThat(awaitItem()).isEqualTo(replacementScreen)
            assertThat(navigationHandler.previousScreen.value).isNull()
            assertThat(navigationHandler.canGoBack).isFalse()
            assertThat(initialScreen.closeCalls.awaitItem()).isEqualTo(Unit)
            initialScreen.validate()
        }
    }

    @Test
    fun `replaceCurrentScreen closes replacement while transition is in progress`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val pendingScreen = FakeScreen()
            navigationHandler.transitionToWithDelay(pendingScreen)
            val droppedReplacement = FakeCloseableScreen()

            val replaced = navigationHandler.replaceCurrentScreen(droppedReplacement)

            assertThat(replaced).isFalse()
            assertThat(droppedReplacement.closeCalls.awaitItem()).isEqualTo(Unit)
            droppedReplacement.validate()
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(pendingScreen)
        }
    }

    @Test
    fun `pop removes the top screen from the backstack`() = runTest {
        var calledPopHandler = false
        val screenOne = FakeScreen()
        val screenTwo = FakeScreen()

        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {
            calledPopHandler = true
            assertThat(it).isEqualTo(screenTwo)
        }

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)

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

        val screenOne = FakeScreen()
        val screenTwo = FakeCloseableScreen()

        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {
            calledPopHandler = true
            assertThat(it).isEqualTo(screenTwo)
        }

        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)

            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)

            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()

            navigationHandler.pop()
            assertThat(calledPopHandler).isTrue()
            assertThat(awaitItem()).isEqualTo(screenOne)
            assertThat(navigationHandler.canGoBack).isFalse()
            assertThat(screenTwo.closeCalls.awaitItem()).isEqualTo(Unit)
            screenTwo.validate()
        }
    }

    @Test
    fun `popWithDelay transitions to target screen`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
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
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            navigationHandler.popWithDelay()
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenThree = FakeScreen()
            navigationHandler.transitionTo(screenThree)
            assertThat(awaitItem()).isEqualTo(screenThree)
            assertThat(navigationHandler.canGoBack).isTrue()
        }
    }

    @Test
    fun `popWithDelay prevents other navigation actions during delay`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            navigationHandler.popWithDelay()
            val screenThree = FakeScreen()
            navigationHandler.transitionTo(screenThree)
            navigationHandler.pop()
            navigationHandler.resetTo(listOf(screenThree))
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(screenOne)
        }
    }

    @Test
    fun `cancelling the coroutine scope closes all closable screens in the backstack`() = runTest {
        val scope = coroutineScopeCleanupRule.track(CoroutineScope(Job()))
        val navigationHandler = NavigationHandler<TestScreen>(scope, LoadingScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val screenOne = FakeCloseableScreen()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = FakeCloseableScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            scope.cancel()
            assertThat(screenOne.closeCalls.awaitItem()).isEqualTo(Unit)
            assertThat(screenTwo.closeCalls.awaitItem()).isEqualTo(Unit)
            screenOne.validate()
            screenTwo.validate()
        }
    }

    @Test
    fun `transitionTo closes the initial screen when it is removed from the backstack`() = runTest {
        val initialScreen = FakeCloseableScreen()
        val navigationHandler = NavigationHandler<TestScreen>(this, initialScreen) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            val newScreen = FakeScreen()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(initialScreen.closeCalls.awaitItem()).isEqualTo(Unit)
            initialScreen.validate()
        }
    }

    @Test
    fun `transitionTo does not close the initial screen when it is kept on the backstack`() = runTest {
        val initialScreen = FakeCloseableScreen()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = this,
            initialScreen = initialScreen,
            shouldRemoveInitialScreenOnTransition = false,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(initialScreen)
            val newScreen = FakeScreen()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            initialScreen.validate()
        }
    }

    @Test
    fun `cancelling the coroutine scope closes a pending delayed transition that has not been applied`() = runTest {
        val scope = coroutineScopeCleanupRule.track(CoroutineScope(Job() + UnconfinedTestDispatcher(testScheduler)))
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = scope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val pendingScreen = FakeCloseableScreen()
            navigationHandler.transitionToWithDelay(pendingScreen)
            // The transition delay has not elapsed, so the target is not on the back stack yet.
            scope.cancel()
            assertThat(pendingScreen.closeCalls.awaitItem()).isEqualTo(Unit)
            pendingScreen.validate()
        }
    }

    @Test
    fun `transitionToWithDelay closes a target dropped while a transition is already in flight`() = runTest {
        val testScope = TestScope()
        val navigationHandler = NavigationHandler<TestScreen>(
            coroutineScope = testScope,
            initialScreen = LoadingScreen,
        ) {}
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(LoadingScreen)
            val firstTarget = FakeCloseableScreen()
            navigationHandler.transitionToWithDelay(firstTarget)
            val droppedTarget = FakeCloseableScreen()
            navigationHandler.transitionToWithDelay(droppedTarget)
            assertThat(droppedTarget.closeCalls.awaitItem()).isEqualTo(Unit)
            droppedTarget.validate()
            firstTarget.validate()
            testScope.testScheduler.advanceTimeBy(251.milliseconds)
            assertThat(awaitItem()).isEqualTo(firstTarget)
        }
    }

    @Test
    fun `cancelling the coroutine scope closes the screens on the backstack`() = runTest {
        val scope = coroutineScopeCleanupRule.track(CoroutineScope(Job()))
        val initialScreen = FakeCloseableScreen()
        NavigationHandler<TestScreen>(scope, initialScreen) {}

        scope.cancel()

        assertThat(initialScreen.closeCalls.awaitItem()).isEqualTo(Unit)
        initialScreen.validate()
    }

    @Test
    fun `previousScreen value is correct`() = runTest {
        val navigationHandler = NavigationHandler<TestScreen>(this, LoadingScreen) {}
        navigationHandler.previousScreen.test {
            // Initially, there is no previous screen.
            assertThat(awaitItem()).isNull()

            val screenOne = FakeScreen()
            navigationHandler.transitionTo(screenOne)
            // The previous screen doesn't get updated here -- Loading is removed from the backstack as part of the
            // initial loading. The previous screen is still null.

            val screenTwo = FakeScreen()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenOne)
        }
    }
}

private interface TestScreen

private object LoadingScreen : TestScreen

private class FakeScreen : TestScreen

private class FakeCloseableScreen : TestScreen, Closeable {
    val closeCalls = Turbine<Unit>()

    override fun close() {
        closeCalls.add(Unit)
    }

    fun validate() {
        closeCalls.ensureAllEventsConsumed()
    }
}

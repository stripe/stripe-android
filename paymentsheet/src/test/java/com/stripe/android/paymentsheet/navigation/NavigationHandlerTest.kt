package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.Closeable

internal class NavigationHandlerTest {
    @Test
    fun `currentScreen is initialized to Loading`() = runTest {
        val navigationHandler = NavigationHandler()
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `transitionTo removes Loading`() = runTest {
        val navigationHandler = NavigationHandler()
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val newScreen = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(newScreen)
            assertThat(awaitItem()).isEqualTo(newScreen)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `transitionTo keeps backstack`() = runTest {
        val navigationHandler = NavigationHandler()
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
    fun `resetTo resets backstack`() = runTest {
        val navigationHandler = NavigationHandler()
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
        val navigationHandler = NavigationHandler()
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
        val navigationHandler = NavigationHandler()
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
        val navigationHandler = NavigationHandler()
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            var calledPopHandler = false
            navigationHandler.pop {
                calledPopHandler = true
                assertThat(it).isEqualTo(screenTwo)
            }
            assertThat(calledPopHandler).isTrue()
            assertThat(awaitItem()).isEqualTo(screenOne)
            assertThat(navigationHandler.canGoBack).isFalse()
        }
    }

    @Test
    fun `pop removes the top screen from the backstack and calls close`() = runTest {
        val navigationHandler = NavigationHandler()
        navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetScreen.Loading)
            val screenOne = mock<PaymentSheetScreen>()
            navigationHandler.transitionTo(screenOne)
            assertThat(awaitItem()).isEqualTo(screenOne)
            val screenTwo = mock<PaymentSheetScreen>(extraInterfaces = arrayOf(Closeable::class))
            navigationHandler.transitionTo(screenTwo)
            assertThat(awaitItem()).isEqualTo(screenTwo)
            assertThat(navigationHandler.canGoBack).isTrue()
            var calledPopHandler = false
            navigationHandler.pop {
                calledPopHandler = true
                assertThat(it).isEqualTo(screenTwo)
            }
            assertThat(calledPopHandler).isTrue()
            assertThat(awaitItem()).isEqualTo(screenOne)
            assertThat(navigationHandler.canGoBack).isFalse()
            verify(screenTwo as Closeable).close()
        }
    }

    @Test
    fun `closeScreens calls close on all closable screens in the backstack`() = runTest {
        val navigationHandler = NavigationHandler()
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

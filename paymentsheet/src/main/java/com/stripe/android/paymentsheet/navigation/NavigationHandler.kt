package com.stripe.android.paymentsheet.navigation

import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

internal class NavigationHandler(
    private val coroutineScope: CoroutineScope,
    private val poppedScreenHandler: (PaymentSheetScreen) -> Unit,
) {
    private val isTransitioning = AtomicBoolean(false)

    private val backStack = MutableStateFlow<List<PaymentSheetScreen>>(
        value = listOf(PaymentSheetScreen.Loading),
    )

    val currentScreen: StateFlow<PaymentSheetScreen> = backStack
        .mapAsStateFlow { it.last() }

    val canGoBack: Boolean
        get() = backStack.value.size > 1

    fun transitionTo(target: PaymentSheetScreen) {
        if (!isTransitioning.get()) {
            transitionToInternal(target)
        }
    }

    fun transitionToWithDelay(target: PaymentSheetScreen) {
        navigateWithDelay { transitionToInternal(target) }
    }

    private fun transitionToInternal(target: PaymentSheetScreen) {
        backStack.update { (it - PaymentSheetScreen.Loading) + target }
    }

    fun resetTo(screens: List<PaymentSheetScreen>) {
        if (!isTransitioning.get()) {
            val previousBackStack = backStack.value

            backStack.value = screens

            previousBackStack.forEach { oldScreen ->
                if (oldScreen !in screens) {
                    oldScreen.onClose()
                }
            }
        }
    }

    fun pop() {
        if (!isTransitioning.get()) {
            popInternal()
        }
    }

    fun popWithDelay() {
        navigateWithDelay { popInternal() }
    }

    private fun popInternal() {
        backStack.update { screens ->
            val modifiableScreens = screens.toMutableList()

            val lastScreen = modifiableScreens.removeLast()

            lastScreen.onClose()

            poppedScreenHandler(lastScreen)

            modifiableScreens.toList()
        }
    }

    fun closeScreens() {
        backStack.value.forEach {
            it.onClose()
        }
    }

    private fun PaymentSheetScreen.onClose() {
        when (this) {
            is Closeable -> close()
            else -> Unit
        }
    }

    private fun navigateWithDelay(action: () -> Unit) {
        if (!isTransitioning.getAndSet(true)) {
            // Introduce a delay to show ripple.
            coroutineScope.launch {
                delay(250.milliseconds)
                action()
                isTransitioning.set(false)
            }
        }
    }
}

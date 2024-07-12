package com.stripe.android.paymentsheet.navigation

import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.Closeable

internal class NavigationHandler(
    private val poppedScreenHandler: (PaymentSheetScreen) -> Unit,
) {
    private val backStack = MutableStateFlow<List<PaymentSheetScreen>>(
        value = listOf(PaymentSheetScreen.Loading),
    )

    val currentScreen: StateFlow<PaymentSheetScreen> = backStack
        .mapAsStateFlow { it.last() }

    val canGoBack: Boolean
        get() = backStack.value.size > 1

    fun transitionTo(target: PaymentSheetScreen) {
        backStack.update { (it - PaymentSheetScreen.Loading) + target }
    }

    fun resetTo(screens: List<PaymentSheetScreen>) {
        val previousBackStack = backStack.value

        backStack.value = screens

        previousBackStack.forEach { oldScreen ->
            if (oldScreen !in screens) {
                oldScreen.onClose()
            }
        }
    }

    fun pop() {
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
}

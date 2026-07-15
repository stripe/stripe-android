package com.stripe.android.paymentsheet.navigation

import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

internal class NavigationHandler<T : Any>(
    private val coroutineScope: CoroutineScope,
    private val initialScreen: T,
    private val shouldRemoveInitialScreenOnTransition: Boolean = true,
    private val poppedScreenHandler: (T) -> Unit,
) {
    private val isTransitioning = AtomicBoolean(false)

    // A screen queued by [transitionToWithDelay] whose transition has not been applied yet. Tracked
    // so it can still be closed if [closeScreens] runs (e.g. the activity is destroyed) before the
    // delayed transition lands it on the back stack.
    @Volatile
    private var pendingTransitionScreen: T? = null

    private val backStack = MutableStateFlow<List<T>>(
        value = listOf(initialScreen),
    )

    val currentScreen: StateFlow<T> = backStack
        .mapAsStateFlow { it.last() }

    val previousScreen: StateFlow<T?> = backStack.mapAsStateFlow {
        if (it.isEmpty() || it.size == 1) {
            // In these cases, there is no "previous screen".
            null
        } else {
            it[it.size - 2]
        }
    }

    val canGoBack: Boolean
        get() = backStack.value.size > 1

    init {
        // Closing is driven by the scope's lifecycle: when the owning scope is canceled (for
        // example when the ViewModel is cleared or the activity is destroyed), close every screen
        // still being tracked. Consumers therefore don't need to remember to call closeScreens().
        coroutineScope.coroutineContext.job.invokeOnCompletion {
            closeScreens()
        }
    }

    fun transitionTo(target: T) {
        if (!isTransitioning.get()) {
            transitionToInternal(target)
        }
    }

    fun transitionToWithDelay(target: T) {
        val scheduled = navigateWithDelay {
            transitionToInternal(target)
            pendingTransitionScreen = null
        }
        if (scheduled) {
            pendingTransitionScreen = target
        } else {
            // A navigation is already in flight, so this target will never be shown. Close it now
            // instead of leaking it, since it never enters the back stack.
            target.onClose()
        }
    }

    private fun transitionToInternal(target: T) {
        if (shouldRemoveInitialScreenOnTransition) {
            val previousBackStack = backStack.value
            backStack.value = (previousBackStack - initialScreen) + target
            if (initialScreen != target && initialScreen in previousBackStack) {
                // The initial screen is dropped from the back stack on the first transition; close
                // it so a Closeable initial screen doesn't leak.
                initialScreen.onClose()
            }
        } else {
            backStack.update { it + target }
        }
    }

    fun resetTo(screens: List<T>) {
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

            val lastScreen = modifiableScreens.removeAt(modifiableScreens.lastIndex)

            lastScreen.onClose()

            poppedScreenHandler(lastScreen)

            modifiableScreens.toList()
        }
    }

    private fun closeScreens() {
        backStack.value.forEach {
            it.onClose()
        }
        // A delayed transition may have been scheduled but not yet applied (e.g. the activity is
        // destroyed within the transition delay). That target never entered the back stack, so
        // close it here too.
        pendingTransitionScreen?.onClose()
        pendingTransitionScreen = null
    }

    private fun T.onClose() {
        when (this) {
            is Closeable -> close()
            else -> Unit
        }
    }

    private fun navigateWithDelay(action: () -> Unit): Boolean {
        return if (!isTransitioning.getAndSet(true)) {
            // Introduce a delay to show ripple.
            coroutineScope.launch {
                delay(250.milliseconds)
                action()
                isTransitioning.set(false)
            }
            true
        } else {
            false
        }
    }
}

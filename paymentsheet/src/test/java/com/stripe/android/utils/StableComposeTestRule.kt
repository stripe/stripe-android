package com.stripe.android.utils

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.coroutines.CoroutineContext

/**
 * Helps make Compose tests stable and avoid AppNotIdleException.
 * Taken from: https://github.com/robolectric/robolectric/issues/7055#issuecomment-1482789098
 */
class StableComposeTestRule : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        executeAnyStuckCoroutines()
    }

    private fun executeAnyStuckCoroutines() {
        val dispatcherClass = dispatcherClass()
        val dispatcher = dispatcher(dispatcherClass)

        val scheduledFrameDispatch = dispatcherClass
            .getDeclaredField("scheduledFrameDispatch")
            .apply { isAccessible = true }
            .getBoolean(dispatcher)

        val scheduledTrampolineDispatch = dispatcherClass
            .getDeclaredField("scheduledTrampolineDispatch")
            .apply { isAccessible = true }
            .getBoolean(dispatcher)

        val dispatchCallback = dispatcherClass
            .getDeclaredField("dispatchCallback")
            .apply { isAccessible = true }
            .get(dispatcher) as Runnable

        if (scheduledFrameDispatch || scheduledTrampolineDispatch) {
            dispatchCallback.run()
        }
    }

    private fun dispatcherClass(): Class<*> {
        return Class.forName("androidx.compose.ui.platform.AndroidUiDispatcher")
    }

    private fun dispatcher(dispatcherClass: Class<*>): Any? {
        val dispatcherCompanion = dispatcherClass.getDeclaredField("Companion").get(dispatcherClass)
        val combinedContext = dispatcherCompanion.javaClass.getDeclaredMethod("getMain")
            .invoke(dispatcherCompanion) as CoroutineContext

        val combinedContextClass = Class.forName("kotlin.coroutines.CombinedContext")
        return dispatcherClass.cast(
            combinedContextClass.getDeclaredField("element").apply {
                isAccessible = true
            }.get(combinedContext)
        )
    }
}

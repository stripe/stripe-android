package com.stripe.android.testing

import com.google.common.truth.Truth.assertThat
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.coroutines.CoroutineContext

/**
 * Clears pending Compose frames between Robolectric tests, matching the rule in stripe-ui-core.
 * See https://github.com/robolectric/robolectric/issues/7055.
 */
private class ComposeCleanupRule : TestWatcher() {
    override fun starting(description: Description?) {
        val clazz = javaClass.classLoader!!.loadClass("androidx.compose.ui.platform.AndroidUiDispatcher")
        val combinedContextClass = javaClass.classLoader!!.loadClass("kotlin.coroutines.CombinedContext")
        val companion = clazz.getDeclaredField("Companion").get(clazz)
        val context = companion.javaClass.getDeclaredMethod("getMain").invoke(companion) as CoroutineContext
        val dispatcher = combinedContextClass.getDeclaredField("element")
            .apply { isAccessible = true }
            .get(context)
            .let { clazz.cast(it) }
        val frameScheduled = clazz.getDeclaredField("scheduledFrameDispatch").apply { isAccessible = true }
        val trampolineScheduled = clazz.getDeclaredField("scheduledTrampolineDispatch").apply { isAccessible = true }
        val callback = clazz.getDeclaredField("dispatchCallback")
            .apply { isAccessible = true }
            .get(dispatcher) as Runnable

        if (frameScheduled.getBoolean(dispatcher) || trampolineScheduled.getBoolean(dispatcher)) {
            callback.run()
        }
        assertThat(frameScheduled.getBoolean(dispatcher)).isFalse()
        assertThat(trampolineScheduled.getBoolean(dispatcher)).isFalse()
    }
}

fun createComposeCleanupRule(): TestWatcher = ComposeCleanupRule()

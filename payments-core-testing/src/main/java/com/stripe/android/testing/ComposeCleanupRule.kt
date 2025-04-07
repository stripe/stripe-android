package com.stripe.android.testing

import junit.framework.Assert.assertFalse
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.coroutines.CoroutineContext

/*
 * This rule is a workaround for https://github.com/robolectric/robolectric/issues/7055. It will cleanup any remaining
 * resources from Jetpack Compose after completing a test. Ideally, this is handled by Robolectric. If Robolectric
 * does handle cleanup in a new version, this rule should be removed.
 *
 * See https://github.com/robolectric/robolectric/issues/7055#issuecomment-1482789098 for explanation on the issue
 * that is occurring.
 *
 * Pulled from: https://github.com/robolectric/robolectric/issues/7055#issuecomment-1551119229
 */
class ComposeCleanupRule internal constructor() : TestWatcher() {
    override fun finished(description: Description?) {
        val clazz = javaClass.classLoader!!.loadClass("androidx.compose.ui.platform.AndroidUiDispatcher")
        val combinedContextClass = javaClass.classLoader!!.loadClass("kotlin.coroutines.CombinedContext")
        val companionClazz = clazz.getDeclaredField("Companion").get(clazz)
        val combinedContext = companionClazz.javaClass.getDeclaredMethod("getMain")
            .invoke(companionClazz) as CoroutineContext

        val androidUiDispatcher = combinedContextClass.getDeclaredField("element")
            .apply { isAccessible = true }
            .get(combinedContext)
            .let { clazz.cast(it) }

        var scheduledFrameDispatch = clazz.getDeclaredField("scheduledFrameDispatch")
            .apply { isAccessible = true }
            .getBoolean(androidUiDispatcher)
        var scheduledTrampolineDispatch = clazz.getDeclaredField("scheduledTrampolineDispatch")
            .apply { isAccessible = true }
            .getBoolean(androidUiDispatcher)

        val dispatchCallback = clazz.getDeclaredField("dispatchCallback")
            .apply { isAccessible = true }
            .get(androidUiDispatcher) as Runnable

        if (scheduledFrameDispatch || scheduledTrampolineDispatch) {
            dispatchCallback.run()
            scheduledFrameDispatch = clazz.getDeclaredField("scheduledFrameDispatch")
                .apply { isAccessible = true }
                .getBoolean(androidUiDispatcher)
            scheduledTrampolineDispatch = clazz.getDeclaredField("scheduledTrampolineDispatch")
                .apply { isAccessible = true }
                .getBoolean(androidUiDispatcher)
        }

        assertFalse(scheduledFrameDispatch)
        assertFalse(scheduledTrampolineDispatch)

        super.finished(description)
    }
}

fun createComposeCleanupRule() = ComposeCleanupRule()

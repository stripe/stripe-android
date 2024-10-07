package com.stripe.android.screenshottesting

import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import com.android.ide.common.rendering.api.RenderSession
import com.android.ide.common.rendering.api.ViewInfo
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

private const val WINDOW_RECOMPOSER_ANDROID_KT_FQN =
    "androidx.compose.ui.platform.WindowRecomposer_androidKt"
private const val COMBINED_CONTEXT_FQN = "kotlin.coroutines.CombinedContext"
private const val SNAPSHOT_KT_FQN = "androidx.compose.runtime.snapshots.SnapshotKt"
private const val PAPARAZZI_COMPOSE_VIEW_ADAPTER_FQN = "app.cash.paparazzi.internal.ComposeViewAdapter"

private val LOGGER = Logger.getLogger("DisposeRenderSession")

/**
 * Initiates a custom [RenderSession] disposal, involving clearing several static collections
 * including some Compose-related objects as well as executing default [RenderSession.dispose].
 *
 * Pulled & modified from the
 * [Paparazzi GitHub issue](https://github.com/cashapp/paparazzi/issues/915#issuecomment-1783114569) in regards to
 * this memory leak with `layoutlib` and `Jetpack Compose` that is based on
 * [Google's own solution](https://issuetracker.google.com/issues/290990640#comment5) for the problem in regards to
 * Android Studio.
 *
 * This should be removed after upgrading Paparazzi to 1.3.4.
 */
internal fun RenderSession.disposeWithCompose() {
    val applyObserversRef = AtomicReference<WeakReference<MutableCollection<*>?>?>(null)
    val toRunTrampolinedRef = AtomicReference<WeakReference<MutableCollection<*>?>?>(null)

    try {
        val windowRecomposer: Class<*> = Class.forName(WINDOW_RECOMPOSER_ANDROID_KT_FQN)
        val animationScaleField = windowRecomposer.getDeclaredField("animationScale")
        animationScaleField.isAccessible = true
        val animationScale = animationScaleField[windowRecomposer]
        if (animationScale is Map<*, *>) {
            (animationScale as MutableMap<*, *>).clear()
        }
    } catch (ex: ReflectiveOperationException) {
        // If the WindowRecomposer does not exist or the animationScale does not exist anymore,
        // ignore.
        LOGGER.warning("Unable to dispose the recompose animationScale $ex")
    }
    applyObserversRef.set(WeakReference(findApplyObservers()))
    toRunTrampolinedRef.set(WeakReference(findToRunTrampolined()))

    execute {
        rootViews
            .filterNotNull()
            .forEach { v -> disposeIfCompose(v) }
    }
    val weakApplyObservers = applyObserversRef.get()
    if (weakApplyObservers != null) {
        val applyObservers = weakApplyObservers.get()
        applyObservers?.clear()
    }
    val weakToRunTrampolined = toRunTrampolinedRef.get()
    if (weakToRunTrampolined != null) {
        val toRunTrampolined = weakToRunTrampolined.get()
        toRunTrampolined?.clear()
    }
    dispose()
}

/**
 * Performs dispose() call against View object associated with [ViewInfo] if that object is an
 * instance of Paparazzi's internal `ComposeViewAdapter` instance.
 *
 * @param viewInfo a [ViewInfo] associated with the View object to be potentially disposed of
 */
private fun disposeIfCompose(viewInfo: ViewInfo) {
    val viewObject: Any? = viewInfo.viewObject
    if (viewObject?.javaClass?.name != PAPARAZZI_COMPOSE_VIEW_ADAPTER_FQN) {
        return
    }
    try {
        val composeView = viewInfo.children[0].viewObject as ComposeView
        composeView.disposeComposition()
    } catch (ex: IllegalAccessException) {
        LOGGER.warning("Unexpected error while disposing compose view $ex")
    } catch (ex: InvocationTargetException) {
        LOGGER.warning("Unexpected error while disposing compose view $ex")
    }
}

private fun findApplyObservers(): MutableCollection<*>? {
    try {
        val applyObserversField = Class.forName(SNAPSHOT_KT_FQN).getDeclaredField("applyObservers")
        applyObserversField.isAccessible = true
        val applyObservers = applyObserversField[null]
        if (applyObservers is MutableCollection<*>) {
            return applyObservers
        }
        LOGGER.warning("SnapshotsKt.applyObservers found but it is not a List")
    } catch (ex: ReflectiveOperationException) {
        LOGGER.warning("Unable to find SnapshotsKt.applyObservers $ex")
    }
    return null
}

private fun findToRunTrampolined(): MutableCollection<*>? {
    try {
        val uiDispatcher = AndroidUiDispatcher::class.java
        val uiDispatcherCompanion = AndroidUiDispatcher.Companion::class.java
        val uiDispatcherCompanionField = uiDispatcher.getDeclaredField("Companion")
        val uiDispatcherCompanionObj = uiDispatcherCompanionField[null]
        val getMainMethod =
            uiDispatcherCompanion.getDeclaredMethod("getMain").apply { isAccessible = true }
        val mainObj = getMainMethod.invoke(uiDispatcherCompanionObj)
        val combinedContext = Class.forName(COMBINED_CONTEXT_FQN)
        val elementField = combinedContext.getDeclaredField("element").apply { isAccessible = true }
        val uiDispatcherObj = elementField[mainObj]

        val toRunTrampolinedField =
            uiDispatcher.getDeclaredField("toRunTrampolined").apply { isAccessible = true }
        val toRunTrampolinedObj = toRunTrampolinedField[uiDispatcherObj]
        if (toRunTrampolinedObj is MutableCollection<*>) {
            return toRunTrampolinedObj
        }
        LOGGER.warning("AndroidUiDispatcher.toRunTrampolined found but it is not a MutableCollection")
    } catch (ex: ReflectiveOperationException) {
        LOGGER.warning("Unable to find AndroidUiDispatcher.toRunTrampolined $ex")
    }
    return null
}

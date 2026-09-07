package com.stripe.android.uicore.elements.bottomsheet

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue.Expanded
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
internal class StripeBottomSheetStateTest {

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `show settles at the expanded anchor when content grows on the final animation frame`() = runScenario {
        advanceFramesUntil { sheet.progress(Hidden, Expanded) in LAST_FRAME_PROGRESS..<1f }

        contentHeight.value = GROWN_CONTENT_HEIGHT
        advanceFrames(1)

        assertWithMessage("The race did not occur: the sheet already sits at the new anchor")
            .that(sheet.progress(Hidden, Expanded))
            .isLessThan(1f)

        advanceFramesUntil { sheet.isVisible }

        assertThat(sheet.isVisible).isTrue()
        assertThat(sheet.progress(Hidden, Expanded)).isEqualTo(1f)
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(FRAME_MILLIS))
        val contentHeight = mutableStateOf(INITIAL_CONTENT_HEIGHT)
        val isSheetComposed = mutableStateOf(false)
        lateinit var state: StripeBottomSheetState

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    if (isSheetComposed.value) {
                        state = rememberStripeBottomSheetState()
                        StripeBottomSheetLayout(
                            state = state,
                            layoutInfo = StripeBottomSheetLayoutInfo(
                                sheetShape = RectangleShape,
                                sheetBackgroundColor = Color.White,
                                scrimColor = Color.Black,
                            ),
                            onDismissed = {},
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(contentHeight.value)
                            )
                        }
                    }
                }
            }

            // A paused Choreographer only runs frames when the test advances the clock. That
            // preserves the on-device ordering within a frame: animation callbacks, then layout,
            // then coroutine resumption. Composing the sheet after the first traversal ensures
            // its anchors exist before the show animation starts, as they do on a device.
            ShadowChoreographer.setPaused(true)
            advanceFrames(1)
            isSheetComposed.value = true
            advanceFrames(1)

            Scenario(
                sheet = state.modalBottomSheetState,
                contentHeight = contentHeight,
            ).block()
        }
    }

    private class Scenario(
        val sheet: ModalBottomSheetState,
        val contentHeight: MutableState<Dp>,
    ) {
        fun advanceFramesUntil(condition: () -> Boolean) {
            repeat(MAX_FRAMES) {
                if (condition()) {
                    return
                }
                advanceFrames(1)
            }
            assertWithMessage("Condition not met after $MAX_FRAMES frames").that(condition()).isTrue()
        }
    }

    private companion object {
        // The sheet animates with a 300 ms tween, so a 50 ms frame at 95% progress is the
        // second-to-last frame, and the next frame is the one that finishes the animation.
        const val FRAME_MILLIS = 50L
        const val LAST_FRAME_PROGRESS = 0.95f
        const val MAX_FRAMES = 100
        val INITIAL_CONTENT_HEIGHT = 100.dp
        val GROWN_CONTENT_HEIGHT = 200.dp

        fun advanceFrames(count: Int) {
            repeat(count) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MILLIS))
            }
        }
    }
}

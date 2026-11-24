package com.stripe.android.paymentelement.embedded.linkinline

import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class EmbeddedVisibilityTest {

    @Test
    fun `element on screen is visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT)).isTrue()
    }

    @Test
    fun `element outside a window isn't visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(isAttachedToWindow = false))).isFalse()
    }

    @Test
    fun `element in an unfocused window isn't visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(hasWindowFocus = false))).isFalse()
    }

    @Test
    fun `element of a paused screen isn't visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(isLifecycleResumed = false))).isFalse()
    }

    @Test
    fun `element under a hidden view isn't visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(isViewShown = false))).isFalse()
    }

    @Test
    fun `element under a transparent view isn't visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(minAncestorAlpha = 0.005f))).isFalse()
    }

    @Test
    fun `element under a nearly opaque view is visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(minAncestorAlpha = 0.01f))).isTrue()
    }

    @Test
    fun `element covered by a sheet or dialog isn't visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(isUiPresentedOverHost = true))).isFalse()
    }

    @Test
    fun `element below the fold isn't visible`() {
        assertThat(EmbeddedVisibility.isVisible(SNAPSHOT.copy(viewVisibleRectInWindow = null))).isFalse()
    }

    @Test
    fun `tall element needs only the capped height on screen`() {
        // 1,000px tall at 2x density: 100dp (200px) on screen is enough.
        val snapshot = SNAPSHOT.copy(
            height = 1_000f,
            boundsInWindow = Rect(0f, 1_000f, 1_000f, 2_000f),
            viewVisibleRectInWindow = Rect(0f, 0f, 1_000f, 1_200f),
        )

        assertThat(EmbeddedVisibility.isVisible(snapshot)).isTrue()
    }

    @Test
    fun `tall element with less than the capped height on screen isn't visible`() {
        val snapshot = SNAPSHOT.copy(
            height = 1_000f,
            boundsInWindow = Rect(0f, 1_000f, 1_000f, 2_000f),
            viewVisibleRectInWindow = Rect(0f, 0f, 1_000f, 1_199f),
        )

        assertThat(EmbeddedVisibility.isVisible(snapshot)).isFalse()
    }

    @Test
    fun `short element needs half its height on screen`() {
        // 300px tall: 150px on screen is enough.
        val snapshot = SNAPSHOT.copy(
            height = 300f,
            boundsInWindow = Rect(0f, 1_000f, 1_000f, 1_300f),
            viewVisibleRectInWindow = Rect(0f, 0f, 1_000f, 1_150f),
        )

        assertThat(EmbeddedVisibility.isVisible(snapshot)).isTrue()
    }

    @Test
    fun `short element with less than half its height on screen isn't visible`() {
        val snapshot = SNAPSHOT.copy(
            height = 300f,
            boundsInWindow = Rect(0f, 1_000f, 1_000f, 1_300f),
            viewVisibleRectInWindow = Rect(0f, 0f, 1_000f, 1_149f),
        )

        assertThat(EmbeddedVisibility.isVisible(snapshot)).isFalse()
    }

    @Test
    fun `element clipped by a scrolling container isn't visible`() {
        // Compose clips the bounds to what's visible in the scroll container.
        val snapshot = SNAPSHOT.copy(
            height = 300f,
            boundsInWindow = Rect(0f, 800f, 1_000f, 900f),
        )

        assertThat(EmbeddedVisibility.isVisible(snapshot)).isFalse()
    }

    private companion object {
        val SNAPSHOT = EmbeddedVisibility.Snapshot(
            isAttachedToWindow = true,
            hasWindowFocus = true,
            isLifecycleResumed = true,
            isViewShown = true,
            minAncestorAlpha = 1f,
            height = 300f,
            boundsInWindow = Rect(0f, 100f, 1_000f, 400f),
            viewVisibleRectInWindow = Rect(0f, 0f, 1_000f, 2_000f),
            isUiPresentedOverHost = false,
            density = 2f,
        )
    }
}

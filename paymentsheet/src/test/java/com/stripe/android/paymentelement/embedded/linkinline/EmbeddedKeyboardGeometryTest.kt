package com.stripe.android.paymentelement.embedded.linkinline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.Decision
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.Span
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthEvent.DialogReason
import org.junit.Test

internal class EmbeddedKeyboardGeometryTest {

    @Test
    fun `last observed keyboard height predicts the next one`() {
        val height = EmbeddedKeyboardGeometry.predictKeyboardHeight(
            lastObservedHeight = 700f,
            isLandscape = false,
            isTablet = false,
            bottomInset = 48f,
            density = 2f,
        )

        assertThat(height).isEqualTo(700f)
    }

    @Test
    fun `portrait keyboard is predicted from its default height`() {
        val height = EmbeddedKeyboardGeometry.predictKeyboardHeight(
            lastObservedHeight = null,
            isLandscape = false,
            isTablet = false,
            bottomInset = 48f,
            density = 2f,
        )

        // 216dp of keys and a 44dp suggestion bar, above the bottom inset.
        assertThat(height).isEqualTo((216f + 44f) * 2f + 48f)
    }

    @Test
    fun `landscape keyboard is predicted from its default height`() {
        val height = EmbeddedKeyboardGeometry.predictKeyboardHeight(
            lastObservedHeight = null,
            isLandscape = true,
            isTablet = false,
            bottomInset = 0f,
            density = 1f,
        )

        assertThat(height).isEqualTo(162f + 44f)
    }

    @Test
    fun `tablet keyboard is predicted from its default height`() {
        val height = EmbeddedKeyboardGeometry.predictKeyboardHeight(
            lastObservedHeight = null,
            isLandscape = true,
            isTablet = true,
            bottomInset = 0f,
            density = 1f,
        )

        assertThat(height).isEqualTo(320f + 44f)
    }

    @Test
    fun `empty observed keyboard height isn't used for predictions`() {
        val height = EmbeddedKeyboardGeometry.predictKeyboardHeight(
            lastObservedHeight = 0f,
            isLandscape = false,
            isTablet = false,
            bottomInset = 0f,
            density = 1f,
        )

        assertThat(height).isEqualTo(216f + 44f)
    }

    @Test
    fun `code is typed in place inside a scroll container it fits in`() {
        val decision = decide(region = Span(1500f, 1700f), hasScrollContainer = true)

        assertThat(decision).isEqualTo(Decision.Inline)
    }

    @Test
    fun `code goes to the dialog without a scroll container when the keyboard would cover it`() {
        val decision = decide(region = Span(1100f, 1300f), hasScrollContainer = false)

        assertThat(decision).isEqualTo(Decision.Dialog(DialogReason.NoScrollView))
    }

    @Test
    fun `code is typed in place without a scroll container when it's above the keyboard`() {
        val decision = decide(region = Span(800f, KEYBOARD_TOP - MARGIN), hasScrollContainer = false)

        assertThat(decision).isEqualTo(Decision.Inline)
    }

    @Test
    fun `code goes to the dialog when it can't fit above the keyboard`() {
        val decision = decide(region = Span(0f, KEYBOARD_TOP), hasScrollContainer = true)

        assertThat(decision).isEqualTo(Decision.Dialog(DialogReason.DoesNotFit))
    }

    @Test
    fun `code goes to the dialog when it isn't on screen`() {
        val decision = decide(region = null, hasScrollContainer = true)

        assertThat(decision).isEqualTo(Decision.Dialog(DialogReason.NotInWindow))
    }

    @Test
    fun `fit check is exact at its boundary`() {
        val available = Span(100f, 500f)

        assertThat(EmbeddedKeyboardGeometry.canFit(height = 368f, available = available, margin = MARGIN)).isTrue()
        assertThat(EmbeddedKeyboardGeometry.canFit(height = 368.1f, available = available, margin = MARGIN)).isFalse()
    }

    @Test
    fun `visible region is brought into view with its margins only`() {
        val span = EmbeddedKeyboardGeometry.bringIntoViewSpan(
            regionHeight = 200f,
            hostBottom = KEYBOARD_TOP,
            keyboardTop = KEYBOARD_TOP,
            margin = MARGIN,
        )

        assertThat(span).isEqualTo(Span(top = -MARGIN, bottom = 200f + MARGIN))
    }

    @Test
    fun `region in a host behind the keyboard is brought into view above it`() {
        val span = EmbeddedKeyboardGeometry.bringIntoViewSpan(
            regionHeight = 200f,
            hostBottom = WINDOW_BOTTOM,
            keyboardTop = KEYBOARD_TOP,
            margin = MARGIN,
        )

        // A scroll container showing the span's bottom at the host's bottom shows the region above the keyboard.
        assertThat(span).isEqualTo(Span(top = -MARGIN, bottom = 200f + MARGIN + (WINDOW_BOTTOM - KEYBOARD_TOP)))
    }

    @Test
    fun `region above the keyboard isn't hidden below`() {
        val obscured = EmbeddedKeyboardGeometry.obscuredBelow(
            region = Span(800f, KEYBOARD_TOP - MARGIN),
            visibleArea = Span(0f, WINDOW_BOTTOM),
            keyboardTop = KEYBOARD_TOP,
            margin = MARGIN,
        )

        assertThat(obscured).isEqualTo(0f)
    }

    @Test
    fun `region behind the keyboard is hidden below by the extra scroll it needs`() {
        val obscured = EmbeddedKeyboardGeometry.obscuredBelow(
            region = Span(1100f, 1300f),
            visibleArea = Span(0f, WINDOW_BOTTOM),
            keyboardTop = KEYBOARD_TOP,
            margin = MARGIN,
        )

        assertThat(obscured).isEqualTo(1300f + MARGIN - KEYBOARD_TOP)
    }

    @Test
    fun `region hidden by the keyboard and clipped above counts both`() {
        val obscured = EmbeddedKeyboardGeometry.obscuredAmount(
            region = Span(50f, 1250f),
            visibleArea = Span(100f, WINDOW_BOTTOM),
            keyboardTop = KEYBOARD_TOP,
        )

        assertThat(obscured).isEqualTo(100f)
    }

    @Test
    fun `fully clipped region is hidden`() {
        val obscured = EmbeddedKeyboardGeometry.obscuredAmount(
            region = Span(100f, 300f),
            visibleArea = null,
            keyboardTop = KEYBOARD_TOP,
        )

        assertThat(obscured).isEqualTo(200f)
    }

    private fun decide(region: Span?, hasScrollContainer: Boolean): Decision {
        return EmbeddedKeyboardGeometry.decide(
            region = region,
            visibleArea = Span(0f, WINDOW_BOTTOM),
            hasScrollContainer = hasScrollContainer,
            keyboardTop = KEYBOARD_TOP,
            margin = MARGIN,
        )
    }

    private companion object {
        const val WINDOW_BOTTOM = 2000f
        const val KEYBOARD_TOP = 1200f
        const val MARGIN = 16f
    }
}

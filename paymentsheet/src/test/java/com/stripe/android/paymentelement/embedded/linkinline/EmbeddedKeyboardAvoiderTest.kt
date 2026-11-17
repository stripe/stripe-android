package com.stripe.android.paymentelement.embedded.linkinline

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardAvoider.Companion.SETTLE_DELAY_MILLIS
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedKeyboardGeometry.Span
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

internal class EmbeddedKeyboardAvoiderTest {

    @Test
    fun `region behind the keyboard scrolls just above it`() = runScenario {
        avoider.start()
        settle()

        host.bringIntoViewCalls.awaitItem()
        host.bringIntoViewCalls.awaitItem()
        assertThat(host.region.bottom).isEqualTo(KEYBOARD_TOP - MARGIN)
        assertThat(avoider.padding.value).isEqualTo(0f)
    }

    @Test
    fun `visible region doesn't move`() = runScenario(regionTop = 800f) {
        avoider.start()
        settle()

        host.bringIntoViewCalls.awaitItem()
        host.bringIntoViewCalls.awaitItem()
        assertThat(host.region.top).isEqualTo(800f)
    }

    @Test
    fun `content that can't scroll far enough gets padding for the extra scroll`() = runScenario(maxScroll = 100f) {
        avoider.start()
        runCurrent()

        host.bringIntoViewCalls.awaitItem()
        // After scrolling 100, the region still needs this much more room above the keyboard.
        val extraScroll = REGION_TOP + REGION_HEIGHT - 100f + MARGIN - KEYBOARD_TOP
        assertThat(avoider.padding.value).isEqualTo(extraScroll)
        host.bringIntoViewCalls.awaitItem()
        assertThat(host.region.bottom).isEqualTo(KEYBOARD_TOP - MARGIN)
    }

    @Test
    fun `host that already makes room for the keyboard gets no padding`() = runScenario(
        hostBottom = KEYBOARD_TOP,
        regionTop = 1100f,
    ) {
        avoider.start()
        settle()

        assertThat(host.bringIntoViewCalls.awaitItem()).isEqualTo(Span(-MARGIN, REGION_HEIGHT + MARGIN))
        host.bringIntoViewCalls.awaitItem()
        assertThat(host.region.bottom).isEqualTo(KEYBOARD_TOP - MARGIN)
        assertThat(avoider.padding.value).isEqualTo(0f)
    }

    @Test
    fun `stopping removes the padding`() = runScenario(maxScroll = 100f) {
        avoider.start()
        runCurrent()
        host.bringIntoViewCalls.awaitItem()
        host.bringIntoViewCalls.awaitItem()

        avoider.stop()

        assertThat(avoider.padding.value).isEqualTo(0f)
    }

    @Test
    fun `keyboard changes before starting move nothing`() = runScenario {
        avoider.onLayoutChanged()
        settle()

        host.bringIntoViewCalls.expectNoEvents()
    }

    @Test
    fun `empty keyboard frame moves nothing and isn't remembered`() = runScenario(keyboardHeight = 0f) {
        avoider.start()
        settle()

        host.bringIntoViewCalls.expectNoEvents()
        assertThat(keyboardHeights.lastObservedDp(isLandscape = false)).isNull()
    }

    @Test
    fun `keyboard height is remembered for the next prediction`() = runScenario {
        avoider.start()
        runCurrent()

        host.bringIntoViewCalls.awaitItem()
        assertThat(keyboardHeights.lastObservedDp(isLandscape = false)).isEqualTo(KEYBOARD_HEIGHT / DENSITY)
        assertThat(keyboardHeights.lastObservedDp(isLandscape = true)).isNull()
    }

    @Test
    fun `region is adjusted again once the keyboard settles`() = runScenario {
        avoider.start()
        runCurrent()
        host.bringIntoViewCalls.awaitItem()
        // The host moves its content itself after the keyboard appears.
        host.moveRegion(by = 100f)

        settle()

        host.bringIntoViewCalls.awaitItem()
        assertThat(host.region.bottom).isEqualTo(KEYBOARD_TOP - MARGIN)
        onObscuredCalls.expectNoEvents()
    }

    @Test
    fun `region that grows while typing is adjusted again`() = runScenario {
        avoider.start()
        settle()
        host.bringIntoViewCalls.awaitItem()
        host.bringIntoViewCalls.awaitItem()

        host.growRegion(by = 40f)
        avoider.onLayoutChanged()
        runCurrent()

        host.bringIntoViewCalls.awaitItem()
        assertThat(host.region.bottom).isEqualTo(KEYBOARD_TOP - MARGIN)
    }

    @Test
    fun `region still hidden after the keyboard settles gives up`() = runScenario(
        maxScroll = 0f,
        hasScrollContainer = false,
    ) {
        avoider.start()
        settle()

        host.bringIntoViewCalls.awaitItem()
        host.bringIntoViewCalls.awaitItem()
        onObscuredCalls.awaitItem()
        assertThat(avoider.padding.value).isEqualTo(0f)

        // Stopped, so later changes move nothing.
        avoider.onLayoutChanged()
        settle()
        host.bringIntoViewCalls.expectNoEvents()
    }

    @Test
    fun `region hidden by less than half a dp is visible enough`() = runScenario(
        maxScroll = 0f,
        hasScrollContainer = false,
        regionTop = KEYBOARD_TOP - REGION_HEIGHT + 0.4f * DENSITY,
    ) {
        avoider.start()
        settle()

        host.bringIntoViewCalls.awaitItem()
        host.bringIntoViewCalls.awaitItem()
        onObscuredCalls.expectNoEvents()
    }

    private fun runScenario(
        regionTop: Float = REGION_TOP,
        hostBottom: Float = WINDOW_BOTTOM,
        keyboardHeight: Float = KEYBOARD_HEIGHT,
        maxScroll: Float = 10_000f,
        hasScrollContainer: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val keyboardHeights = EmbeddedKeyboardHeights()
        val onObscuredCalls = Turbine<Unit>()
        lateinit var avoider: EmbeddedKeyboardAvoider
        val host = FakeScrollHost(
            regionTop = regionTop,
            hostBottom = hostBottom,
            keyboardHeight = keyboardHeight,
            maxScroll = maxScroll,
            hasScrollContainer = hasScrollContainer,
            padding = { avoider.padding.value },
        )
        avoider = EmbeddedKeyboardAvoider(
            measure = host::measure,
            bringIntoView = host::bringIntoView,
            awaitLayout = {},
            keyboardHeights = keyboardHeights,
            onObscured = { onObscuredCalls.add(Unit) },
            scope = backgroundScope,
        )

        Scenario(
            avoider = avoider,
            host = host,
            keyboardHeights = keyboardHeights,
            onObscuredCalls = onObscuredCalls,
            testScope = this,
        ).block()

        host.bringIntoViewCalls.ensureAllEventsConsumed()
        onObscuredCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val avoider: EmbeddedKeyboardAvoider,
        val host: FakeScrollHost,
        val keyboardHeights: EmbeddedKeyboardHeights,
        val onObscuredCalls: Turbine<Unit>,
        private val testScope: TestScope,
    ) {
        fun runCurrent() = testScope.runCurrent()

        /**
         * Lets the keyboard settle, then runs the checks after it.
         */
        fun settle() {
            testScope.advanceTimeBy(SETTLE_DELAY_MILLIS)
            testScope.runCurrent()
        }
    }

    /**
     * A scroll container that brings spans into view the way Compose does by default, unaware of the keyboard.
     * Padding below the element adds to how far it can scroll.
     */
    private class FakeScrollHost(
        regionTop: Float,
        private val hostBottom: Float,
        private val keyboardHeight: Float,
        private val maxScroll: Float,
        private val hasScrollContainer: Boolean,
        private val padding: () -> Float,
    ) {
        val bringIntoViewCalls = Turbine<Span>()
        var region = Span(regionTop, regionTop + REGION_HEIGHT)
            private set
        private var scroll = 0f

        fun measure() = EmbeddedKeyboardAvoider.Measurement(
            region = region,
            regionVisibleArea = Span(max(region.top, 0f), min(region.bottom, hostBottom)).takeIf { it.height > 0f },
            hostVisibleArea = Span(0f, hostBottom),
            keyboardTop = WINDOW_BOTTOM - keyboardHeight,
            keyboardHeight = keyboardHeight,
            hasScrollContainer = hasScrollContainer,
            isLandscape = false,
            density = DENSITY,
        )

        fun bringIntoView(span: Span) {
            bringIntoViewCalls.add(span)
            val top = region.top + span.top
            val bottom = region.top + span.bottom
            val delta = when {
                top >= 0f && bottom <= hostBottom -> 0f
                bottom > hostBottom -> min(bottom - hostBottom, top)
                else -> top
            }
            val scrolled = delta.coerceIn(-scroll, maxScroll + padding() - scroll)
            scroll += scrolled
            moveRegion(by = -scrolled)
        }

        fun moveRegion(by: Float) {
            region = Span(region.top + by, region.bottom + by)
        }

        fun growRegion(by: Float) {
            region = Span(region.top, region.bottom + by)
        }
    }

    private companion object {
        const val DENSITY = 2f
        const val MARGIN = EmbeddedKeyboardGeometry.MARGIN_DP * DENSITY
        const val WINDOW_BOTTOM = 2000f
        const val KEYBOARD_HEIGHT = 800f
        const val KEYBOARD_TOP = WINDOW_BOTTOM - KEYBOARD_HEIGHT
        const val REGION_TOP = 1300f
        const val REGION_HEIGHT = 200f
    }
}

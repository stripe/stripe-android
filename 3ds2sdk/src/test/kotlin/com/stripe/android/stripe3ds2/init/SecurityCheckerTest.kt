package com.stripe.android.stripe3ds2.init

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class SecurityCheckerTest {

    @Test
    fun `warnings with real checks is empty`() {
        assertThat(DefaultSecurityChecker().getWarnings())
            .isEmpty()
    }

    @Test
    fun `warnings with failing checks is not empty`() {
        val passingCheck = SecurityCheck.Emulator()
        val failingCheck = SecurityCheck.DebuggerAttached(true)

        val checks = listOf(
            passingCheck,
            failingCheck
        )

        assertThat(DefaultSecurityChecker(checks).getWarnings())
            .containsExactly(failingCheck.warning)
    }
}

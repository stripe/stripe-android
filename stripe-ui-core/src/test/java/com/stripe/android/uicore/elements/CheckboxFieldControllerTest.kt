package com.stripe.android.uicore.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckboxFieldControllerTest {
    @Test
    fun `on init, controller checked state should be initialized to false`() {
        val controller = CheckboxFieldController()

        assertThat(controller.isChecked.value).isFalse()
    }

    @Test
    fun `on init with initial value set to true, controller checked state should be initialized to true`() {
        val controller = CheckboxFieldController(initialValue = true)

        assertThat(controller.isChecked.value).isTrue()
    }

    @Test
    fun `on value change, check state value should be updated`() {
        val controller = CheckboxFieldController()

        assertThat(controller.isChecked.value).isFalse()

        controller.onValueChange(value = true)

        assertThat(controller.isChecked.value).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on value change from true then back to false, error should be propagated`() = runTest {
        val controller = CheckboxFieldController()

        controller.error.test {
            // Consume initial null event
            awaitItem()

            controller.onValueChange(value = true)

            // Consume next null event
            awaitItem()

            controller.onValueChange(value = false)

            val value = awaitItem()

            assertThat(value?.errorMessage).isEqualTo(R.string.stripe_field_required)
        }
    }
}

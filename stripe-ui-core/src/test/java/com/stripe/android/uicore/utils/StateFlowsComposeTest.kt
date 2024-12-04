package com.stripe.android.uicore.utils

import androidx.compose.material.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.InternalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StateFlowsComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @OptIn(InternalForInheritanceCoroutinesApi::class)
    fun `Custom 'collectAsState' only produces initial value on initial composition`() {
        val stateFlow = MutableStateFlow(2)
        var initialValueProducedCount = 0

        val testStateFlow = object : StateFlow<Int> by stateFlow {
            override val value: Int
                get() {
                    initialValueProducedCount++

                    return stateFlow.value
                }
        }

        val text = mutableStateOf("Hello")
        var recompositionCount = 0

        composeTestRule.setContent {
            val textState by remember {
                text
            }

            val testState by testStateFlow.collectAsState()

            SideEffect {
                recompositionCount++
            }

            Text(textState)
            Text(recompositionCount.toString())
            Text(testState.toString())
        }

        text.value = "my"
        composeTestRule.waitUntilExactlyOneExists(hasText("my"))

        text.value = "name"
        composeTestRule.waitUntilExactlyOneExists(hasText("name"))

        text.value = "is"
        composeTestRule.waitUntilExactlyOneExists(hasText("is"))

        text.value = "John"
        composeTestRule.waitUntilExactlyOneExists(hasText("John"))

        assertThat(recompositionCount).isEqualTo(5)
        assertThat(initialValueProducedCount).isEqualTo(1)
    }

    @Test
    fun `Custom 'collectAsState' recomposes on state updates`() {
        val testStateFlow = MutableStateFlow("Hello")

        composeTestRule.setContent {
            val testState by testStateFlow.collectAsState()

            Text(testState)
        }

        testStateFlow.value = "my"
        composeTestRule.waitUntilExactlyOneExists(hasText("my"))

        testStateFlow.value = "name"
        composeTestRule.waitUntilExactlyOneExists(hasText("name"))

        testStateFlow.value = "is"
        composeTestRule.waitUntilExactlyOneExists(hasText("is"))

        testStateFlow.value = "John"
        composeTestRule.waitUntilExactlyOneExists(hasText("John"))
    }
}

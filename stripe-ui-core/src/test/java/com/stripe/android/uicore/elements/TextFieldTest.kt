package com.stripe.android.uicore.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasInsertTextAtCursorAction
import androidx.compose.ui.test.hasRequestFocusAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `On initial value passed, selection should be at the end of the field`() {
        composeTestRule.setContent {
            TestTextField(initialValue = "A1B")
        }

        composeTestRule
            .onNodeWithTag(TEST_TAG)
            .assert(hasTextSelection(TextRange(3)))
    }

    @Test
    fun `On input, selection should move with input`() {
        composeTestRule.setContent {
            TestTextField(initialValue = "A1B")
        }

        val textField = composeTestRule.onNodeWithTag(TEST_TAG)

        textField.performTextInput("2")
        textField.performTextInput("C")

        textField.assert(hasTextSelection(TextRange(5)))
        textField.assert(hasText("A1B2C"))
    }

    @Test
    fun `On input in middle of text, selection should move with input`() {
        composeTestRule.setContent {
            TestTextField(initialValue = "A1B")
        }

        val textField = composeTestRule.onNodeWithTag(TEST_TAG)

        textField.performTextInputSelection(TextRange(1))

        textField.performTextInput("2")
        textField.performTextInput("C")

        textField.assert(hasTextSelection(TextRange(3)))
        textField.assert(hasText("A2C1B"))
    }

    @Test
    fun `On text complete, should be able to change selection without changing text`() {
        composeTestRule.setContent {
            TestTextField(initialValue = null)
        }

        val textField = composeTestRule.onNodeWithTag(TEST_TAG)

        textField.performTextInput("A1B2C3")
        textField.assert(hasText("A1B2C3"))

        textField.performTextInputSelection(TextRange(4))
        textField.assert(hasTextSelection(TextRange(4)))

        textField.performTextInputSelection(TextRange(2))
        textField.assert(hasTextSelection(TextRange(2)))

        textField.performTextInputSelection(TextRange(1, 4))
        textField.assert(hasTextSelection(TextRange(1, 4)))
    }

    @Test
    fun `On text complete and attempted input, selection should not change`() {
        composeTestRule.setContent {
            TestTextField(
                initialValue = null,
                maxInputLength = 6
            )
        }

        val textField = composeTestRule.onNodeWithTag(TEST_TAG)

        textField.performTextInput("A1B2C3")
        textField.performTextInput("D4E")

        textField.assert(hasTextSelection(TextRange(6)))
        textField.assert(hasText("A1B2C3"))
    }

    @Test
    fun `On text complete and attempted input from middle of text, selection should not change`() {
        composeTestRule.setContent {
            TestTextField(
                initialValue = null,
            )
        }

        val textField = composeTestRule.onNodeWithTag(TEST_TAG)

        textField.performTextInput("A1B2C3")
        textField.performTextInputSelection(TextRange(3))
        textField.performTextInput("D4E")

        textField.assert(hasTextSelection(TextRange(3)))
        textField.assert(hasText("A1B2C3"))
    }

    @Test
    fun `Test TextFieldUi remembers composition`() {
        val testTag = "TEST"
        val value = MutableStateFlow(
            TextFieldValue(
                text = "",
            )
        )
        val simpleTextFieldController = SimpleTextFieldController(
            textFieldConfig = TestTextFieldConfig(
                maxInputLength = 6
            ),
            initialValue = ""
        )
        composeTestRule.setContent {

            val state by simpleTextFieldController.fieldState.collectAsState()
            val textFieldValue by value.collectAsState()
            TextFieldUi(
                label = "Search",
                value = textFieldValue,
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state,
                modifier = Modifier.testTag(testTag)
            )
        }

        val textField = composeTestRule.onNodeWithTag(testTag)
        textField.assert(hasText(""))

        val composition = TextRange(0,5)
        value.value = TextFieldValue(
            text = "Hello",
            selection = TextRange("Hello".length),
            composition = composition
        )

        textField.assert(hasText("Hello"))
        textField.assert(hasImeComposition(composition))

    }

    private fun hasImeComposition(composition: TextRange): SemanticsMatcher {
        return SemanticsMatcher.expectValue(ImeCompositionKey, composition.toString())
    }


    @Composable
    private fun TestTextField(
        initialValue: String?,
        maxInputLength: Int = 6,
    ) {
        TextField(
            modifier = Modifier.testTag(TEST_TAG),
            textFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = maxInputLength
                ),
                initialValue = initialValue
            ),
            enabled = true,
            imeAction = ImeAction.Next,
        )
    }

    internal class TestTextFieldConfig(
        private val maxInputLength: Int
    ) : TextFieldConfig {
        override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Characters
        override val debugLabel: String = TEST_TAG
        override val label: Int? = null
        override val keyboard: KeyboardType = KeyboardType.Text
        override val visualTransformation: VisualTransformation? = null
        override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)
        override val loading: StateFlow<Boolean> = MutableStateFlow(false)

        override fun determineState(input: String): TextFieldState {
            return if (input.length == maxInputLength) {
                TextFieldStateConstants.Valid.Full
            } else if (input.length > maxInputLength) {
                TextFieldStateConstants.Error.Invalid(
                    R.string.default_error_message
                )
            } else {
                TextFieldStateConstants.Error.Incomplete(
                    R.string.default_error_message
                )
            }
        }

        override fun filter(userTyped: String) = userTyped

        override fun convertToRaw(displayName: String) = displayName

        override fun convertFromRaw(rawValue: String) = rawValue
    }

    private fun hasTextSelection(range: TextRange): SemanticsMatcher {
        return SemanticsMatcher.expectValue(TextSelectionRange, range)
    }

    private fun SemanticsNodeInteraction.performTextInputSelection(
        selection: TextRange
    ) {
        val node = fetchSemanticsNode(ERROR_ON_FAIL)

        assert(isEnabled()) { ERROR_ON_FAIL }
        assert(hasSetTextAction()) { ERROR_ON_FAIL }
        assert(hasRequestFocusAction()) { ERROR_ON_FAIL }
        assert(hasInsertTextAtCursorAction()) { ERROR_ON_FAIL }

        if (!isFocused().matches(node)) {
            performSemanticsAction(SemanticsActions.RequestFocus)
        }

        performSemanticsAction(SemanticsActions.SetSelection) {
            it(selection.min, selection.max, true)
        }
    }

    companion object {
        private const val TEST_TAG = "CORE_TEXT_FIELD"
        private const val ERROR_ON_FAIL = "Failed to perform text selection."
    }
}

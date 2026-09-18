package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetLineContent
import com.stripe.android.identity.viewmodel.BottomSheetViewModel
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
@ExperimentalMaterialApi
@OptIn(ExperimentalCoroutinesApi::class)
internal class BottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private companion object {
        const val TITLE = "TITLE"
        const val BOTTOM_SHEET_ID = "bottom_sheet_id_1"

        const val LINE_TITLE = "LINE_TITLE"
        const val LINE_CONTENT = "this is the content of line"
        const val MULTI_LINES_COUNT = 5

        val SINGLE_LINE_CONTENT = VerificationPageStaticContentBottomSheetContent(
            bottomSheetId = BOTTOM_SHEET_ID,
            title = TITLE,
            lines = listOf(
                VerificationPageStaticContentBottomSheetLineContent(
                    icon = VerificationPageIconType.CLOUD,
                    title = LINE_TITLE,
                    content = LINE_CONTENT
                )
            )
        )

        val MULTI_LINES_CONTENT = VerificationPageStaticContentBottomSheetContent(
            bottomSheetId = BOTTOM_SHEET_ID,
            title = TITLE,
            lines = List(MULTI_LINES_COUNT) {
                VerificationPageStaticContentBottomSheetLineContent(
                    icon = VerificationPageIconType.CLOUD,
                    title = LINE_TITLE,
                    content = LINE_TITLE
                )
            }
        )

        val TITLE_LESS_CONTENT = VerificationPageStaticContentBottomSheetContent(
            bottomSheetId = BOTTOM_SHEET_ID,
            title = null,
            lines = List(MULTI_LINES_COUNT) {
                VerificationPageStaticContentBottomSheetLineContent(
                    icon = VerificationPageIconType.CLOUD,
                    title = LINE_TITLE,
                    content = LINE_TITLE
                )
            }
        )
    }

    @Test
    fun testDismiss() {
        setComposeTestRuleWith(
            viewModelBlock = {
                it.dismissBottomSheet()
            }
        ) {
            onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun testWithSingleLine() {
        setComposeTestRuleWith(
            viewModelBlock = {
                it.showBottomSheet(SINGLE_LINE_CONTENT)
            }
        ) {
            onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).assertExists()
            onNodeWithTag(BOTTOM_SHEET_TITLE_TAG).assertTextEquals(TITLE)
            onAllNodesWithTag(BOTTOM_SHEET_LINE_TAG).assertCountEquals(1)
            onNodeWithTag(BOTTOM_SHEET_BUTTON_TAG).assertExists()
        }
    }

    @Test
    fun testWithMultiLines() {
        setComposeTestRuleWith(
            viewModelBlock = {
                it.showBottomSheet(MULTI_LINES_CONTENT)
            }
        ) {
            onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).assertExists()
            onNodeWithTag(BOTTOM_SHEET_TITLE_TAG).assertTextEquals(TITLE)
            onAllNodesWithTag(BOTTOM_SHEET_LINE_TAG).assertCountEquals(MULTI_LINES_COUNT)
            onNodeWithTag(BOTTOM_SHEET_BUTTON_TAG).assertExists()
        }
    }

    @Test
    fun testWithNoTitles() {
        setComposeTestRuleWith(
            viewModelBlock = {
                it.showBottomSheet(TITLE_LESS_CONTENT)
            }
        ) {
            onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).assertExists()
            onNodeWithTag(BOTTOM_SHEET_TITLE_TAG).assertDoesNotExist()
            onAllNodesWithTag(BOTTOM_SHEET_LINE_TAG).assertCountEquals(MULTI_LINES_COUNT)
            onNodeWithTag(BOTTOM_SHEET_BUTTON_TAG).assertExists()
        }
    }

    @Test
    fun `close keeps content until the sheet finishes hiding`() {
        lateinit var viewModel: BottomSheetViewModel
        setComposeTestRuleWith(
            viewModelBlock = {
                viewModel = it
                it.showBottomSheet(SINGLE_LINE_CONTENT)
            }
        ) {
            val initialBounds = onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).getUnclippedBoundsInRoot()
            onNodeWithTag(BOTTOM_SHEET_BUTTON_TAG).performClick()

            assertThat(viewModel.bottomSheetState.value.shouldShow).isFalse()
            onNodeWithTag(BOTTOM_SHEET_TITLE_TAG).assertTextEquals(TITLE)
            assertThat(onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).getUnclippedBoundsInRoot())
                .isEqualTo(initialBounds)

            runOnIdle { viewModel.onBottomSheetHidden() }

            onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).assertDoesNotExist()
            assertThat(viewModel.bottomSheetState.value.content).isNull()
        }
    }

    @Test
    fun `hiding without a close request clears content and visibility`() {
        lateinit var viewModel: BottomSheetViewModel
        setComposeTestRuleWith(
            viewModelBlock = {
                viewModel = it
                it.showBottomSheet(SINGLE_LINE_CONTENT)
            }
        ) {
            onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).assertExists()
            assertThat(viewModel.bottomSheetState.value.shouldShow).isTrue()

            runOnIdle { viewModel.onBottomSheetHidden() }

            onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).assertDoesNotExist()
            assertThat(viewModel.bottomSheetState.value.shouldShow).isFalse()
            assertThat(viewModel.bottomSheetState.value.content).isNull()
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h1000dp")
    fun `sheet grows beyond 400 dp without scrolling when content fits`() = runLayoutScenario(lineCount = 6) {
        val scrollable = onNode(hasScrollAction())
        val scrollRange = scrollable.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]

        assertThat(scrollable.getUnclippedBoundsInRoot().height.value).isGreaterThan(400f)
        assertThat(scrollRange.maxValue()).isEqualTo(0f)
        assertThat(onNodeWithTag(BOTTOM_SHEET_CONTENT_TAG).getUnclippedBoundsInRoot().height.value)
            .isLessThan(800f)
        onNodeWithTag(BOTTOM_SHEET_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h1000dp")
    fun `overflow scrolls at available height while close remains visible`() = runLayoutScenario(lineCount = 30) {
        val scrollable = onNode(hasScrollAction())
        val scrollRange = scrollable.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]

        assertThat(scrollable.getUnclippedBoundsInRoot().height.value).isGreaterThan(400f)
        assertThat(scrollRange.maxValue()).isGreaterThan(0f)
        onNodeWithTag(BOTTOM_SHEET_BUTTON_TAG).assertIsDisplayed()
        onAllNodesWithTag(BOTTOM_SHEET_LINE_TAG)[29].performScrollTo().assertIsDisplayed()
        onNodeWithTag(BOTTOM_SHEET_BUTTON_TAG).assertIsDisplayed()
    }

    private fun runLayoutScenario(
        lineCount: Int,
        testBlock: ComposeContentTestRule.() -> Unit
    ) {
        composeTestRule.setContent {
            val viewModel = viewModel<BottomSheetViewModel>()
            viewModel.showBottomSheet(
                SINGLE_LINE_CONTENT.copy(lines = List(lineCount) { SINGLE_LINE_CONTENT.lines.single() })
            )
            Box(Modifier.height(800.dp)) {
                BottomSheet()
            }
        }

        with(composeTestRule, testBlock)
    }

    private fun setComposeTestRuleWith(
        viewModelBlock: (BottomSheetViewModel) -> Unit,
        testBlock: ComposeContentTestRule.() -> Unit
    ) {
        composeTestRule.setContent {
            val viewModel = viewModel<BottomSheetViewModel>()
            viewModelBlock(viewModel)
            BottomSheet()
        }

        with(composeTestRule, testBlock)
    }
}

package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetLineContent
import com.stripe.android.identity.viewmodel.BottomSheetViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
@ExperimentalMaterialApi
internal class BottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

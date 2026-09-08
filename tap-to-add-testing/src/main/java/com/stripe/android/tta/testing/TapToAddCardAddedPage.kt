package com.stripe.android.tta.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_CARD_ADDED_PRIMARY_BUTTON
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_CARD_ADDED_SHOWN_DELAY
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_LAYOUT_TEST_TAG
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickEnabledNode
import com.stripe.android.testing.waitForExactlyOneNode
import com.stripe.android.testing.waitForNoNodes

class TapToAddCardAddedPage(
    private val composeTestRule: ComposeTestRule,
    private val linkHelper: TapToAddLinkTestHelper,
) {
    private val primaryButtonElement = TapToAddPrimaryButtonElement(composeTestRule)

    fun assertShown(
        withLink: Boolean = false,
    ) {
        assertHasCardAddedText()

        if (withLink) {
            linkHelper.checkbox().assertExists()
        }

        if (withLink) {
            assertHasContinueButton()
        } else {
            primaryButtonElement.assertNotShown()
        }
    }

    fun clickCheckboxToSaveWithLink() {
        composeTestRule.clickEnabledNode(
            node = linkHelper.checkbox(),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun fillLinkInput() {
        linkHelper.fillEmail()
        linkHelper.fillPhone()
        linkHelper.fillName()
    }

    fun assertContinueButton(isEnabled: Boolean) {
        assertHasContinueButton().run {
            if (isEnabled) {
                assertIsEnabled()
            } else {
                assertIsNotEnabled()
            }
        }
    }

    fun clickContinue() {
        composeTestRule.clickEnabledNode(
            node = assertHasContinueButton(),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun clickCloseButton() {
        composeTestRule.clickEnabledNode(
            node = composeTestRule.retrieveCloseButton(),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun advancePastScreen() {
        composeTestRule.mainClock.advanceTimeBy(TAP_TO_ADD_CARD_ADDED_SHOWN_DELAY)
    }

    fun waitUntilMissing() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(TAP_TO_ADD_CARD_ADDED_PRIMARY_BUTTON)
                .or(hasTestTag(TAP_TO_ADD_LAYOUT_TEST_TAG)),
            atLeastOneRootRequired = false,
        )
    }

    private fun assertHasCardAddedText() {
        val matcher = hasText("Card added")

        composeTestRule.waitForExactlyOneNode(
            matcher = matcher,
            atLeastOneRootRequired = false,
        )

        composeTestRule.onNode(matcher).assertIsDisplayed()
    }

    private fun assertHasContinueButton() = primaryButtonElement.assert(withLabel = "Continue")
}

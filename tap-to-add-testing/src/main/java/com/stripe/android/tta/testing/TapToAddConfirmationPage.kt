package com.stripe.android.tta.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_CONFIRMATION_PRIMARY_BUTTON
import com.stripe.android.common.taptoadd.ui.TAP_TO_ADD_LAYOUT_TEST_TAG
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickEnabledNode
import com.stripe.android.testing.inputText
import com.stripe.android.testing.waitForNoNodes

class TapToAddConfirmationPage(
    private val composeTestRule: ComposeTestRule,
) {
    private val primaryButtonElement = TapToAddPrimaryButtonElement(composeTestRule)

    fun assertPrimaryButton(withLabel: String? = null, isEnabled: Boolean = true) {
        primaryButtonElement.assert(withLabel).run {
            if (isEnabled) {
                assertIsEnabled()
            } else {
                assertIsNotEnabled()
            }
        }
    }

    fun assertErrorMessageShown(message: String) {
        composeTestRule.onNode(hasText(message)).assertIsDisplayed()
    }

    fun assertCvcRecollectionFieldShown() {
        retrieveCvcField()
            .assertExists()
            .assertIsEnabled()
    }

    fun fillCvc(cvc: String) {
        composeTestRule.inputText(
            node = retrieveCvcField(),
            text = cvc,
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun clickPrimaryButton() {
        composeTestRule.clickEnabledNode(
            node = primaryButtonElement.assert(null),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun waitUntilMissing() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(TAP_TO_ADD_CONFIRMATION_PRIMARY_BUTTON)
                .or(hasTestTag(TAP_TO_ADD_LAYOUT_TEST_TAG)),
            atLeastOneRootRequired = false,
        )
    }

    fun clickCloseButton() {
        composeTestRule.clickEnabledNode(
            node = composeTestRule.retrieveCloseButton(),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    private fun retrieveCvcField(): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasText("CVC"))
    }
}

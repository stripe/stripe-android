package com.stripe.android.paymentsheet

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
internal class PaymentMethodsUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val bancontactTestTag by lazy {
        TEST_TAG_LIST + composeTestRule.activity.resources.getString(
            SupportedPaymentMethod.Bancontact.displayNameResource
        )
    }
    private val epsTestTag by lazy {
        TEST_TAG_LIST + composeTestRule.activity.resources.getString(
            SupportedPaymentMethod.Eps.displayNameResource
        )
    }

    @Test
    fun test() {
        composeTestRule.setContent {
            PaymentMethodsUI(
                paymentMethods = listOf(
                    SupportedPaymentMethod.Bancontact,
                    SupportedPaymentMethod.Sofort,
                    SupportedPaymentMethod.AfterpayClearpay,
                    SupportedPaymentMethod.Eps
                ),
                selectedIndex = 0,
                isEnabled = true,
                onItemSelectedListener = {}
            )
        }

        // Expect the value to be equal to the width of the screen
        // minus the left and right padding for each card
        // calculateViewWidth(
        //     composeTestRule.activity.resources.displayMetrics,
        //     paymentMethods.size
        // ) - (CARD_HORIZONTAL_PADDING.dp * 2)
        bancontact.assertWidthIsEqualTo(109.454544.dp)
    }

    @Test
    fun testScrollIsDisabledWhenListDisabled() {
        val enableListControl = MutableStateFlow(true)
        composeTestRule.setContent {
            val enabled by enableListControl.collectAsState(true)
            PaymentMethodsUI(
                paymentMethods = listOf(
                    SupportedPaymentMethod.Bancontact,
                    SupportedPaymentMethod.SepaDebit,
                    SupportedPaymentMethod.Sofort,
                    SupportedPaymentMethod.Ideal,
                    SupportedPaymentMethod.Eps
                ),
                selectedIndex = 0,
                isEnabled = enabled,
                onItemSelectedListener = {}
            )
        }

        // scroll to the EPS label, which is not currently in view, bancontact the first item
        // will no longer be in view
        bancontact.assertIsDisplayed()
        eps.assertDoesNotExist()
        paymentMethodList.performScrollToNode(hasTestTag(epsTestTag))
        bancontact.assertIsNotDisplayed()
        eps.assertIsDisplayed()

        // When we disable the list no longer scroll to the first item in the list
        enableListControl.value = false
        bancontact.assertExists()
        bancontact.assertIsNotDisplayed()
        paymentMethodList.assertScrollToListItemDisabled(bancontactTestTag)
        bancontact.assertIsNotDisplayed()
        eps.assertIsDisplayed()
    }

    /**
     * Disabling of scrolling will fail because it will not be able to find the node
     * in the scroll list.
     */
    private fun SemanticsNodeInteraction.assertScrollToListItemDisabled(testTag: String) {
        var errorMsg: String? = null
        try {
            this.performScrollToNode(
                hasTestTag(testTag)
            )
        } catch (e: AssertionError) {
            errorMsg = e.message
        }
        assert(
            errorMsg?.contains(
                "No node found that matches TestTag = '${testTag}' in scrollable container"
            ) ?: false
        )
    }

    private val bancontact
        get() = composeTestRule.onNodeWithTag(
            TEST_TAG_LIST + composeTestRule.activity.resources.getString(
                SupportedPaymentMethod.Bancontact.displayNameResource
            )
        )

    private val eps
        get() = composeTestRule.onNodeWithTag(
            TEST_TAG_LIST + composeTestRule.activity.resources.getString(
                SupportedPaymentMethod.Eps.displayNameResource
            )
        )

    private val paymentMethodList
        get() = composeTestRule.onNodeWithTag(TEST_TAG_LIST)
}

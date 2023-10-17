package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SepaMandateActivityTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun testActivityIsFinishedWhenNoArgsPassed() {
        val scenario = ActivityScenario.launchActivityForResult(SepaMandateActivity::class.java)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        val sepaMandateContract = SepaMandateContract()
        val result = sepaMandateContract.parseResult(
            resultCode = scenario.result.resultCode,
            intent = scenario.result.resultData,
        )
        assertThat(result).isEqualTo(SepaMandateResult.Canceled)
    }

    @Test
    fun testActivityReturnsCanceledResultOnBackPressed() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val args = SepaMandateContract.Args("Example, Inc.")
        val sepaMandateContract = SepaMandateContract()
        val intent = sepaMandateContract.createIntent(context, args)
        val scenario = ActivityScenario.launchActivityForResult<SepaMandateActivity>(intent)
        Espresso.pressBack()
        val result = sepaMandateContract.parseResult(
            resultCode = scenario.result.resultCode,
            intent = scenario.result.resultData,
        )
        assertThat(result).isEqualTo(SepaMandateResult.Canceled)
    }

    @Test
    fun testContinueReturnsAcknowledgedResult() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val args = SepaMandateContract.Args("Example, Inc.")
        val sepaMandateContract = SepaMandateContract()
        val intent = sepaMandateContract.createIntent(context, args)
        val scenario = ActivityScenario.launchActivityForResult<SepaMandateActivity>(intent)
        composeRule.onNode(hasTestTag("SEPA_MANDATE_CONTINUE_BUTTON")).assertIsDisplayed()
        composeRule.onNode(hasTestTag("SEPA_MANDATE_CONTINUE_BUTTON")).performClick()
        val result = sepaMandateContract.parseResult(
            resultCode = scenario.result.resultCode,
            intent = scenario.result.resultData,
        )
        assertThat(result).isEqualTo(SepaMandateResult.Acknowledged)
    }
}

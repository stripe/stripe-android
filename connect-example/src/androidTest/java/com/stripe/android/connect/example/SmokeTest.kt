package com.stripe.android.connect.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stripe.android.connect.example.data.FakeEmbeddedComponentService
import com.stripe.android.connect.example.util.Fixtures
import com.stripe.android.connect.example.util.onNodeWithTextRes
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var embeddedComponentService: FakeEmbeddedComponentService

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testAppOpensToLoading() {
        composeTestRule.onNodeWithTextRes(R.string.connect_sdk_example).assertIsDisplayed()
        composeTestRule.onNodeWithTextRes(R.string.warming_up_the_server).assertIsDisplayed()
    }

    @Test
    fun testOpenComponentListWhenDataLoads() {
        embeddedComponentService.publishableKey.value = "fake_key"
        embeddedComponentService.accounts.value = listOf(Fixtures.merchant())

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onNodeWithTextRes(R.string.account_onboarding).isDisplayed()
        }
    }
}

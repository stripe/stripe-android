package com.stripe.android.connect

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.DriverAtoms.webScrollIntoView
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(PrivateBetaConnectSDK::class)
class FullScreenComponentTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(EmptyActivity::class.java)

    private val title = "Test title"

    private lateinit var manager: EmbeddedComponentManager
    private lateinit var controller: AccountOnboardingController

    private val toolbar
        get() = onView(withId(R.id.toolbar))
            .inRoot(RootMatchers.isDialog())

    @Before
    fun setup() {
        activityRule.scenario.onActivity { activity ->
            manager = EmbeddedComponentManager(
                configuration = EmbeddedComponentManager.Configuration(
                    publishableKey = "fake_pk"
                ),
                fetchClientSecretCallback = { it.onResult("fake_secret") },
            )
            controller = manager.createAccountOnboardingController(
                activity = activity,
                title = title,
            )
            controller.show()
        }
    }

    @After
    fun cleanup() {
        activityRule.scenario.onActivity {
            controller.dismiss()
        }
    }

    @Test
    fun testShowWithTitleAndDummyPage() {
        toolbar.check(matches(isDisplayed()))
            .check { view, _ -> assertEquals((view as Toolbar).title, title) }
        onWebView()
            // Sanity check that dummy page is loaded.
            .withElement(findElement(Locator.ID, "top"))
            .check(webMatches(getText(), equalTo("Top")))
    }

    @Test
    fun testScrolling() {
        toolbar.check(matches(isDisplayed()))
        onWebView()
            // Verify we can scroll to bottom.
            .withElement(findElement(Locator.ID, "bottom"))
            .perform(webScrollIntoView())
            .perform(webClick())
            // Verify we can scroll back to top.
            .withElement(findElement(Locator.ID, "top"))
            .perform(webScrollIntoView())
            .perform(webClick())
    }
}

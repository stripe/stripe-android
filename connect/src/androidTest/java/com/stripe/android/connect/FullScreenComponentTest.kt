package com.stripe.android.connect

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.script
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.DriverAtoms.webScrollIntoView
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
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

    private val rootView
        get() = isAssignableFrom(StripeComponentDialogFragmentView::class.java)

    private val toolbar
        get() = allOf(withId(R.id.toolbar), isDescendantOfA(rootView))

    private val toolbarNavigationButton
        get() = allOf(withParent(toolbar), withClassName(containsString("ImageButton")))

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
        checkDialogIsDisplayed()
        onView(toolbar)
            .check { view, _ -> assertEquals((view as Toolbar).title, title) }
        onWebView()
            // Sanity check that dummy page is loaded.
            .withElement(findElement(Locator.ID, "top"))
            .check(webMatches(getText(), equalTo("Top")))
    }

    @Test
    fun testScrolling() {
        checkDialogIsDisplayed()
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

    @Test
    fun testControllerDismisses() {
        checkDialogIsDisplayed()
        controller.dismiss()
        checkDialogDoesNotExist()
    }

    @Test
    fun testCloseButtonDismissesByDefault() {
        checkDialogIsDisplayed()
        onView(toolbarNavigationButton).perform(click())
        checkDialogDoesNotExist()
    }

    @Test
    fun testBackButtonDismissesByDefault() {
        checkDialogIsDisplayed()
        Espresso.pressBack()
        checkDialogDoesNotExist()
    }

    @Test
    fun testJsCloseWebViewDismisses() {
        checkDialogIsDisplayed()
        onWebView().perform(script("Android.closeWebView()"))
        checkDialogDoesNotExist()
    }

    private fun checkDialogIsDisplayed() {
        onView(rootView)
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
    }

    private fun checkDialogDoesNotExist() {
        onView(rootView)
            .noActivity()
            .check(doesNotExist())
    }
}

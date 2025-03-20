package com.stripe.android.connect

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
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
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.script
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.DriverAtoms.webScrollIntoView
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.stripe.android.connect.webview.serialization.AlertJs
import com.stripe.android.connect.webview.serialization.ConnectJson
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

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

    @Test
    fun testCustomJsAlertContents() {
        checkDialogIsDisplayed()
        val alertJs = AlertJs(
            title = randomString(),
            message = randomString(),
            buttons = AlertJs.ButtonsJs(
                ok = randomString(),
                cancel = randomString(),
            ),
        )
        performWebViewAlert(ALERT, alertJs)
        onView(withText(alertJs.title)).check(matches(isDisplayed()))
        onView(withText(alertJs.message)).check(matches(isDisplayed()))
        onView(withText(alertJs.buttons!!.ok)).check(matches(isDisplayed()))
        onView(withText(alertJs.buttons.cancel)).check(matches(isDisplayed()))
    }

    @Test
    fun testCustomJsAlertDismissal() {
        checkDialogIsDisplayed()
        val alertJs = AlertJs(
            title = null,
            message = "alert message",
            buttons = AlertJs.ButtonsJs(
                ok = "ok button",
                cancel = "cancel button",
            ),
        )
        val dismissActions = listOf(
            { onView(withText(alertJs.buttons!!.ok)).perform(click()) },
            { onView(withText(alertJs.buttons!!.cancel)).perform(click()) },
            { Espresso.pressBack() },
        )
        listOf(ALERT, CONFIRM).forEach { method ->
            dismissActions.forEach { dismiss ->
                performWebViewAlert(method, alertJs)
                dismiss()
                checkDialogIsDisplayed()
            }
        }
    }

    private fun randomString() = UUID.randomUUID().toString()

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

    private fun performWebViewAlert(method: String, alertJs: AlertJs) {
        val alertString = ConnectJson.encodeToString(alertJs)
        try {
            onWebView()
                .withTimeout(500L, TimeUnit.MILLISECONDS)
                .perform(script("""$method(`$alertString`)"""))
        } catch (_: NoMatchingViewException) {
            // HACK: After the JS executes, the alert dialog prevents the WebView from
            //  being interacted with. Assume the alert was shown -- we'll be verifying
            //  the dialog below anyway.
        }
    }

    companion object {
        private const val ALERT = "alert"
        private const val CONFIRM = "confirm"
    }
}

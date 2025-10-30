package com.stripe.android.paymentmethodmessaging.element

import android.app.Application
import android.os.Bundle
import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.getCurrentUrl
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LearnMoreActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
//    @get:Rule
//    val composeRule = createComposeRule()
    @get:Rule
    val composeTestRule = createEmptyComposeRule()
    @Test
    fun loadsUrl() {
        val intent = LearnMoreActivityArgs.createIntent(
            applicationContext,
            LearnMoreActivityArgs("file:///android_asset/test.html")
        )
        composeTestRule.launch<
//        ActivityScenario.launch<LearnMoreActivity>(intent).use { activityScenario ->
//            activityScenario.moveToState(Lifecycle.State.CREATED)
////            onWebView().check(webMatches(getCurrentUrl(), containsString("www.test.com")))
////            activityScenario.onActivity { activity ->
////                // Find the WebView. In this activity the WebView is created inside AndroidView,
////                // so we need to search the view hierarchy for the WebView instance.
////                val root = activity.window.decorView
////                val webView = findWebViewInView(root)
////                requireNotNull(webView) { "WebView not found in activity view hierarchy" }
////
////                // The WebView's url property should reflect the loaded URL.
////                // Note: depending on timing, you may want to wait for page load completion.
////                // For a more robust check, you can set a WebViewClient in the test and block until onPageFinished.
////                val loadedUrl = webView.url
////                assertThat(loadedUrl).isEqualTo("file:///android_asset/test.html")
////            }
////            onView(withText("Hello World")).check(matches(isDisplayed()))
////            activityScenario.onActivity {
////                //composeRule.onNodeWithText("Hello World").assertExists()
////                //onWebView().check(webMatches(getCurrentUrl(), containsString("www.test.com")))
////                onView(withText("Hello World")).check(matches(isDisplayed()))
////            }
//
//        }
    }

    private fun findWebViewInView(root: android.view.View): WebView? {
        if (root is WebView) return root
        if (root !is android.view.ViewGroup) return null
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            val found = findWebViewInView(child)
            if (found != null) return found
        }
        return null
    }
}
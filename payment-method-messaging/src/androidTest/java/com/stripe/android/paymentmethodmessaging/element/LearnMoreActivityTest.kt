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
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LearnMoreActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun loadsUrl() {
        val intent = LearnMoreActivityArgs.createIntent(
            applicationContext,
            LearnMoreActivityArgs("file:///android_asset/test.html")
        )
        ActivityScenario.launch<LearnMoreActivity>(intent).use { activityScenario ->
            onWebView().check(webMatches(getCurrentUrl(), Matchers.`is`("file:///android_asset/test.html")))
        }
    }
}
package com.stripe.android.paymentmethodmessaging.element

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.model.Atoms.getCurrentUrl
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LearnMoreActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun loadsUrl() {
        val intent = LearnMoreActivityArgs.createIntent(
            applicationContext,
            LearnMoreActivityArgs("file:///android_asset/fake_web_content.html")
        )
        ActivityScenario.launch<LearnMoreActivity>(intent).use {
            onWebView().check(
                webMatches(
                    getCurrentUrl(),
                    containsString("file:///android_asset/fake_web_content.html")
                )
            )
            .withElement(findElement(Locator.ID, "hello_text"))
            .check(webMatches(getText(), containsString("Hello World")))
        }
    }

    @Test
    fun finishesIfArgsAreNull() {
        ActivityScenario.launch(
            LearnMoreActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun finishesIfUrlIsBlank() {
        val intent = LearnMoreActivityArgs.createIntent(
            applicationContext,
            LearnMoreActivityArgs("")
        )
        ActivityScenario.launch<LearnMoreActivity>(intent).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }
}

package com.stripe.android.financialconnections

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FinancialConnectionsSheetRedirectActivityTest {

    private val packageName = ApplicationProvider.getApplicationContext<Application>().packageName

    @Before
    fun setUp() {
        Intents.init()
        mockLaunchIntents()
    }

    @Test
    fun financialConnectionsSheetRedirectActivity_success_opens_FinancialConnectionsSheetActivity() {
        launchActivity("stripe-auth://link-accounts/$packageName/success").use {
            assertEquals(it.state, Lifecycle.State.DESTROYED)
            intended(hasComponent(FinancialConnectionsSheetActivity::class.java.name))
        }
    }

    @Test
    fun financialConnectionsSheetRedirectActivity_cancel_opens_FinancialConnectionsSheetActivity() {
        launchActivity("stripe-auth://link-accounts/$packageName/cancel").use {
            assertEquals(it.state, Lifecycle.State.DESTROYED)
            intended(hasComponent(FinancialConnectionsSheetActivity::class.java.name))
        }
    }

    @Test
    fun financialConnectionsSheetRedirectActivity_native_app2app_return_opens_FinancialConnectionsSheetNativeActivity() {
        launchActivity("stripe-auth://link-native-accounts/$packageName/authentication_return").use {
            assertEquals(it.state, Lifecycle.State.DESTROYED)
            intended(hasComponent(FinancialConnectionsSheetNativeActivity::class.java.name))
        }
    }

    @Test
    fun financialConnectionsSheetRedirectActivity_web_app2app_return_opens_FinancialConnectionsSheetActivity() {
        launchActivity("stripe-auth://link-accounts/$packageName/authentication_return").use {
            assertEquals(it.state, Lifecycle.State.DESTROYED)
            intended(hasComponent(FinancialConnectionsSheetActivity::class.java.name))
        }
    }

    /**
     * Don't actually open intents, as this just tests redirections happen.
     */
    private fun mockLaunchIntents() {
        val intentResult = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasComponent(FinancialConnectionsSheetNativeActivity::class.java.name))
            .respondWith(intentResult)
        intending(hasComponent(FinancialConnectionsSheetActivity::class.java.name))
            .respondWith(intentResult)
    }

    private fun launchActivity(deeplink: String): ActivityScenario<FinancialConnectionsSheetRedirectActivity> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplink))
            .setPackage(packageName)
        return ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        Intents.release()
    }
}

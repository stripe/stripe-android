package com.stripe.android.connect.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.data.FakeEmbeddedComponentService
import com.stripe.android.connect.example.data.Merchant
import com.stripe.android.connect.example.util.Fixtures
import com.stripe.android.connect.example.util.onNodeWithTextRes
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

abstract class BaseConnectTest {

    @get:Rule(order = 0)
    val hiltRule by lazy { HiltAndroidRule(this) }

    @get:Rule(order = 1)
    val composeTestRule by lazy { createAndroidComposeRule<MainActivity>() }

    @Inject
    lateinit var embeddedComponentService: FakeEmbeddedComponentService

    @Before
    fun init() {
        hiltRule.inject()
    }

    protected fun loadData(merchants: List<Merchant> = listOf(Fixtures.merchant())) {
        embeddedComponentService.publishableKey.value = "fake_key"
        embeddedComponentService.accounts.value = Success(merchants)
    }

    protected inner class MainContext {
        val titleText get() = composeTestRule.onNodeWithTextRes(R.string.connect_sdk_example)
        val loadingText get() = composeTestRule.onNodeWithTextRes(R.string.warming_up_the_server)
    }

    @Suppress("TestFunctionName")
    protected fun Main(f: MainContext.() -> Unit) {
        MainContext().apply(f)
    }

    protected inner class ComponentListContext {
        val accountOnboardingItem get() = composeTestRule.onNodeWithTextRes(R.string.account_onboarding)

        fun openSettings() {
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
        }
    }

    @Suppress("TestFunctionName")
    protected fun ComponentList(f: ComponentListContext.() -> Unit) {
        ComponentListContext().apply(f)
    }

    protected inner class SettingsContext {
        val demoAccountHeader get() = composeTestRule.onNodeWithTextRes(R.string.select_demo_account)

        fun hasAccount(id: String) {
            composeTestRule.onNodeWithText(id).assertIsDisplayed()
        }
    }

    @Suppress("TestFunctionName")
    protected fun Settings(f: SettingsContext.() -> Unit) {
        SettingsContext().apply(f)
    }
}

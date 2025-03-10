package com.stripe.android.connect.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.data.FakeEmbeddedComponentService
import com.stripe.android.connect.example.util.Fixtures
import com.stripe.android.connect.example.util.onNodeWithTextRes
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
abstract class ConnectTest {

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

    protected fun loadData() {
        embeddedComponentService.publishableKey.value = "fake_key"
        embeddedComponentService.accounts.value = Success(listOf(Fixtures.merchant()))
    }

    inner class MainContext {
        val titleText get() = composeTestRule.onNodeWithTextRes(R.string.connect_sdk_example)
        val loadingText get() = composeTestRule.onNodeWithTextRes(R.string.warming_up_the_server)
    }

    protected fun Main(f: MainContext.() -> Unit) {
        MainContext().apply(f)
    }

    inner class ComponentListContext {
        val accountOnboardingItem get() = composeTestRule.onNodeWithTextRes(R.string.account_onboarding)

        fun openSettings() {
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
        }
    }

    protected fun ComponentList(f: ComponentListContext.() -> Unit) {
        ComponentListContext().apply(f)
    }

    inner class SettingsContext {
        val demoAccountHeader get() = composeTestRule.onNodeWithTextRes(R.string.select_demo_account)

        fun hasAccount(id: String) {
            composeTestRule.onNodeWithText(id).assertIsDisplayed()
        }
    }

    protected fun Settings(f: SettingsContext.() -> Unit) {
        SettingsContext().apply(f)
    }
}
